package io.smallrye.reactive.operators.multi;

import static io.smallrye.reactive.helpers.Subscriptions.CANCELLED;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.helpers.queues.SpscArrayQueue;
import io.smallrye.reactive.subscription.BackPressureFailure;

/**
 * Emits events from upstream on a thread managed by the given scheduler.
 *
 * @param <T> the type of item
 */
public class MultiEmitOnOp<T> extends AbstractMultiOperator<T, T> {

    private final Executor executor;
    private final Supplier<? extends Queue<T>> queueSupplier = () -> new SpscArrayQueue<>(16);

    public MultiEmitOnOp(Multi<? extends T> upstream, Executor executor) {
        super(upstream);
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        upstream.subscribe(new MultiEmitOnProcessor<>(downstream, executor, queueSupplier));
    }

    static final class MultiEmitOnProcessor<T> extends MultiOperatorProcessor<T, T> implements Runnable {

        private final Executor executor;

        private final int limit;

        private final Queue<T> queue;

        private volatile boolean cancelled;

        private volatile boolean done;

        private Throwable failure;

        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicLong requested = new AtomicLong();

        private long produced;

        MultiEmitOnProcessor(Subscriber<? super T> downstream,
                Executor executor,
                Supplier<? extends Queue<T>> queueSupplier) {
            super(downstream);
            this.executor = executor;
            this.limit = 16;
            this.queue = queueSupplier.get();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(16);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (!queue.offer(t)) {
                onError(new BackPressureFailure("Queue is full, the upstream didn't enforce the requests"));
                done = true;
                return;
            }

            schedule();
        }

        @Override
        public void onError(Throwable throwable) {
            Subscription subscription = upstream.getAndSet(CANCELLED);
            if (subscription != CANCELLED) {
                done = true;
                failure = throwable;
                schedule();
                subscription.cancel();
            }
        }

        @Override
        public void onComplete() {
            Subscription subscription = upstream.getAndSet(CANCELLED);
            if (subscription != CANCELLED) {
                done = true;
                schedule();
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscription subscription = upstream.get();
                if (subscription != CANCELLED) {
                    Subscriptions.add(requested, n);
                    schedule();
                }
            }
        }

        @Override
        public void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            Subscriptions.cancel(upstream);
            if (wip.getAndIncrement() == 0) {
                queue.clear();
            }
        }

        void schedule() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            try {
                executor.execute(this);
            } catch (RejectedExecutionException rejected) {
                Subscription s = upstream.getAndSet(CANCELLED);
                if (s != CANCELLED) {
                    done = true;
                    queue.clear();
                    downstream.onError(rejected);
                    super.cancel();
                }
            }
        }

        @Override
        public void run() {
            int missed = 1;

            final Queue<T> q = queue;

            long emitted = produced;

            for (;;) {
                long requests = requested.get();
                while (emitted != requests) {
                    boolean wasDone = done;
                    T item;
                    try {
                        item = q.poll();
                    } catch (Throwable ex) {
                        super.cancel();
                        queue.clear();
                        super.onError(ex);
                        return;
                    }

                    boolean none = item == null;

                    if (isDoneOrCancelled(wasDone, none)) {
                        return;
                    }

                    if (none) {
                        break;
                    }

                    downstream.onNext(item);

                    emitted++;
                    if (emitted == limit) {
                        if (requests != Long.MAX_VALUE) {
                            requests = requested.addAndGet(-emitted);
                        }
                        super.request(emitted);
                        emitted = 0L;
                    }
                }

                if (emitted == requests && isDoneOrCancelled(done, q.isEmpty())) {
                    return;
                }

                int w = wip.get();
                if (missed == w) {
                    produced = emitted;
                    missed = wip.addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        boolean isDoneOrCancelled(boolean upstreamDone, boolean queueEmpty) {
            if (cancelled) {
                queue.clear();
                return true;
            }

            // Failure and completion must wait until we are actually done consuming the items.
            if (upstreamDone && queueEmpty) {
                if (failure != null) {
                    downstream.onError(failure);
                } else {
                    downstream.onComplete();
                }
                return true;
            }
            return false;
        }
    }
}
