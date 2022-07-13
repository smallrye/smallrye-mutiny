
package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiWindowOnDurationOp<T> extends AbstractMultiOperator<T, Multi<T>> {

    private final Duration duration;
    private final ScheduledExecutorService executor;

    public MultiWindowOnDurationOp(Multi<T> upstream, Duration duration, ScheduledExecutorService executor) {
        super(upstream);
        this.duration = ParameterValidation.validate(duration, "duration");
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    @Override
    public void subscribe(MultiSubscriber<? super Multi<T>> actual) {
        upstream.subscribe().withSubscriber(new WindowTimeoutSubscriber<>(actual, duration, executor));
    }

    static final class WindowTimeoutSubscriber<T> extends MultiOperatorProcessor<T, Multi<T>> {

        private final Duration duration;
        private final ScheduledExecutorService scheduler;
        private final Queue<Object> queue;

        private Throwable failure;
        private UnicastProcessor<T> current;

        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private final TaskHolder timer = new TaskHolder();

        volatile boolean done;
        volatile boolean terminated;

        WindowTimeoutSubscriber(MultiSubscriber<? super Multi<T>> downstream, Duration duration,
                ScheduledExecutorService scheduler) {
            super(downstream);
            this.queue = Queues.createMpscQueue();
            this.duration = duration;
            this.scheduler = scheduler;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (compareAndSetUpstreamSubscription(null, s)) {
                downstream.onSubscribe(this);

                if (isCancelled()) {
                    return;
                }

                UnicastProcessor<T> w = UnicastProcessor.create();
                current = w;

                long r = requested.get();
                if (r != 0L) {
                    downstream.onNext(w);
                    if (r != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                } else {
                    downstream.onFailure(new BackPressureFailure("no requests"));
                    return;
                }

                if (timer.replace(newPeriod())) {
                    s.request(Long.MAX_VALUE);
                }
            }
        }

        Future<?> newPeriod() {
            try {
                return scheduler
                        .scheduleAtFixedRate(new Tick(this), duration.toMillis(), duration.toMillis(),
                                TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                downstream.onFailure(e);
                return TaskHolder.NONE;
            }
        }

        @Override
        public void onItem(T item) {
            if (terminated) {
                return;
            }

            if (wip.compareAndSet(0, 1)) {
                UnicastProcessor<T> w = current;
                w.onNext(item);
                if (wip.decrementAndGet() == 0) {
                    return;
                }
            } else {
                queue.offer(item);
                if (!canStartWork()) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onFailure(Throwable t) {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                done = true;
                failure = t;
                if (canStartWork()) {
                    drainLoop();
                }

                downstream.onFailure(t);
                timer.cancel();
            } else {
                Infrastructure.handleDroppedException(t);
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                done = true;
                if (canStartWork()) {
                    drainLoop();
                }

                downstream.onCompletion();
                timer.cancel();
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
            }
        }

        @SuppressWarnings("unchecked")
        void drainLoop() {
            final Queue<Object> q = queue;
            final MultiSubscriber<? super Multi<T>> actual = downstream;
            UnicastProcessor<T> processor = current;

            int missed = 1;
            for (;;) {

                for (;;) {
                    if (terminated) {
                        super.cancel();
                        q.clear();
                        timer.cancel();
                        return;
                    }
                    boolean d = done;
                    Object o = q.poll();
                    boolean empty = o == null;
                    boolean isTick = o instanceof WindowTimeoutSubscriber.Tick;

                    if (d && (empty || isTick)) {
                        current = null;
                        q.clear();
                        Throwable err = failure;
                        if (err != null) {
                            processor.onError(err);
                        } else {
                            processor.onComplete();
                        }
                        timer.cancel();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (isTick) {
                        processor.onComplete();
                        processor = UnicastProcessor.create();
                        current = processor;

                        long requests = requested.get();
                        if (requests != 0L) {
                            actual.onItem(processor);
                            if (requests != Long.MAX_VALUE) {
                                requested.decrementAndGet();
                            }
                        } else {
                            current = null;
                            queue.clear();
                            actual.onError(new BackPressureFailure("no requests"));
                            timer.cancel();
                            return;
                        }
                        continue;
                    }

                    processor.onNext((T) o);
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean canStartWork() {
            return wip.getAndIncrement() == 0;
        }

        static final class Tick implements Runnable {

            private final WindowTimeoutSubscriber<?> parent;

            Tick(WindowTimeoutSubscriber<?> parent) {
                this.parent = parent;
            }

            @Override
            public void run() {
                WindowTimeoutSubscriber<?> p = parent;

                if (!p.isCancelled()) {
                    p.queue.offer(this);
                } else {
                    p.terminated = true;
                    p.timer.cancel();
                }
                if (p.canStartWork()) {
                    p.drainLoop();
                }
            }
        }
    }

    private static class TaskHolder {
        private final AtomicReference<Future<?>> container = new AtomicReference<>();

        static final Future<?> NONE = new CompletableFuture<>();

        boolean replace(Future<?> task) {
            for (;;) {
                Future current = container.get();
                if (current == NONE) {
                    if (task != null) {
                        task.cancel(true);
                    }
                    return false;
                }
                if (container.compareAndSet(current, task)) {
                    return true;
                }
            }
        }

        void cancel() {
            Future task = container.getAndSet(NONE);
            if (task != null && task != NONE) {
                task.cancel(false);
            }
        }
    }

}
