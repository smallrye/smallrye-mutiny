package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Emits events from upstream on a thread managed by the given scheduler.
 *
 * @param <T> the type of item
 */
public class MultiEmitOnOp<T> extends AbstractMultiOperator<T, T> {

    private final Executor executor;
    private final Supplier<? extends Queue<T>> queueSupplier = Queues.get(Queues.BUFFER_S);

    public MultiEmitOnOp(Multi<? extends T> upstream, Executor executor) {
        super(upstream);
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "subscriber");
        upstream.subscribe().withSubscriber(new MultiEmitOnProcessor<>(downstream, executor, queueSupplier));
    }

    static final class MultiEmitOnProcessor<T> extends MultiOperatorProcessor<T, T> implements Runnable {

        private final Executor executor;

        private final int limit;

        // State variables

        /**
         * Store the items
         */
        private final Queue<T> queue;

        /**
         * {@code true} if the subscription has been cancelled.
         */
        private volatile boolean cancelled;

        /**
         * {@code true} if no more items should be received (failure or completion received)
         */
        private volatile boolean done;

        /**
         * Stores the failure
         */
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicLong requested = new AtomicLong();

        private long produced;

        MultiEmitOnProcessor(MultiSubscriber<? super T> downstream,
                Executor executor,
                Supplier<? extends Queue<T>> queueSupplier) {
            super(downstream);
            this.executor = executor;
            this.limit = 16;
            this.queue = queueSupplier.get();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(16);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            if (done) {
                // we should not receive any items.
                return;
            }

            if (!queue.offer(t)) {
                // queue full, this is a failure.
                // onError will schedule.
                cancelUpstream(); // cancel upstream
                onFailure(new BackPressureFailure("Queue is full, the upstream didn't enforce the requests"));
                done = true;
            } else {
                schedule();
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            if (!done || !cancelled) {
                done = true;
                failure.set(throwable);
                schedule();
            }
        }

        @Override
        public void onCompletion() {
            if (!done || !cancelled) {
                done = true;
                schedule();
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (!done || !cancelled) {
                    Subscriptions.add(requested, n);
                    schedule();
                }
            } else {
                onFailure(Subscriptions.getInvalidRequestException());
            }
        }

        @Override
        public void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            cancelUpstream();
            if (wip.getAndIncrement() == 0) {
                // nothing was currently dispatched, clearing the queue.
                queue.clear();
            }
        }

        void schedule() {
            if (wip.getAndIncrement() != 0) {
                // we already have a thread running the loop
                return;
            }
            // create a new thread.
            try {
                executor.execute(this);
            } catch (RejectedExecutionException rejected) {
                Flow.Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
                if (subscription != CANCELLED) {
                    done = true;
                    cancelUpstream();
                    queue.clear();
                    downstream.onFailure(rejected);
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
                    T item = q.poll();

                    boolean empty = item == null;
                    if (isDoneOrCancelled(wasDone, empty)) {
                        // we are done.
                        return;
                    }

                    if (empty) {
                        // queue is empty.
                        break;
                    }

                    // Emitting item
                    downstream.onItem(item);

                    // updating the number of emitted items.
                    emitted++;
                    if (emitted == limit) {
                        if (requests != Long.MAX_VALUE) {
                            requests = requested.addAndGet(-emitted);
                        }
                        // request another batch
                        super.request(emitted);
                        emitted = 0L;
                    }
                }

                // we have emitted `limits` items, reached the end of the queue, or reached the number of requests
                // check if we are down for now (requests meet) or for ever (cancelled or done)
                if (emitted == requests && isDoneOrCancelled(done, q.isEmpty())) {
                    return;
                }

                // check if we still have missed notifications.
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

            Throwable maybeFailure = failure.get();
            if (upstreamDone && maybeFailure != null) {
                // failing
                downstream.onFailure(maybeFailure);
                return true;
            }

            // Failure and completion must wait until we are actually done consuming the items.
            if (upstreamDone && queueEmpty) {
                downstream.onCompletion();
                return true;
            }

            return false;
        }
    }
}
