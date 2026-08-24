
package io.smallrye.mutiny.operators.multi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SerializedSubscriber;

/**
 * Buffers items from upstream for a given duration and emits the <em>groups</em> as a single item downstream.
 * If the amount of accumulated reach the given size before the timeout, the group is emitted.
 * This implementation uses {@link java.util.ArrayList} and so emits {@link List}.
 *
 * @param <T> the type of item from upstream
 */
public final class MultiBufferWithTimeoutOp<T> extends AbstractMultiOperator<T, List<T>> {

    private final int size;
    private final Supplier<List<T>> supplier;
    private final ScheduledExecutorService scheduler;
    private final Duration timeout;
    private final boolean emitEmptyListIfNoItem;

    public MultiBufferWithTimeoutOp(Multi<T> upstream,
            int size,
            Duration timeout,
            ScheduledExecutorService scheduler,
            boolean emitEmptyListIfNoItem) {
        super(upstream);
        this.timeout = ParameterValidation.validate(timeout, "timeout");
        this.size = ParameterValidation.positive(size, "size");
        this.scheduler = ParameterValidation.nonNull(scheduler, "scheduler");
        this.emitEmptyListIfNoItem = emitEmptyListIfNoItem;
        this.supplier = () -> {
            if (size < Integer.MAX_VALUE) {
                // Not used yet, on the roadmap.
                return new ArrayList<>(size);
            } else {
                return new ArrayList<>();
            }
        };
    }

    @Override
    public void subscribe(MultiSubscriber<? super List<T>> downstream) {
        MultiBufferWithTimeoutProcessor<T> subscriber = new MultiBufferWithTimeoutProcessor<>(
                new SerializedSubscriber<>(downstream), size, timeout, scheduler, supplier, emitEmptyListIfNoItem);
        upstream.subscribe().withSubscriber(subscriber);
    }

    static class MultiBufferWithTimeoutProcessor<T> extends MultiOperatorProcessor<T, List<T>> {

        private static final int RUNNING = 0;
        private static final int SUCCEED = 1;
        private static final int FAILED = 2;
        private static final int CANCELLED = 3;

        private final int size;
        private final Duration duration;
        private final ScheduledExecutorService executor;
        private final Supplier<List<T>> supplier;

        private final AtomicInteger terminated = new AtomicInteger(RUNNING);
        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger index = new AtomicInteger();
        private final AtomicLong generation = new AtomicLong();
        private final boolean emitEmptyListIfNoItem;
        private List<T> current;
        private volatile ScheduledFuture<?> task;

        MultiBufferWithTimeoutProcessor(MultiSubscriber<? super List<T>> downstream, int size, Duration timeout,
                ScheduledExecutorService executor, Supplier<List<T>> supplier, boolean emitEmptyListIfNoItem) {
            super(downstream);
            this.duration = timeout;
            this.executor = executor;
            this.supplier = supplier;
            this.size = size;
            this.emitEmptyListIfNoItem = emitEmptyListIfNoItem;
        }

        private void scheduleFlush() {
            long gen = generation.incrementAndGet();
            try {
                task = executor.schedule(() -> timerFired(gen), duration.toMillis(), TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException rejected) {
                onFailure(rejected);
            }
        }

        private void timerFired(long expectedGen) {
            if (terminated.get() != RUNNING) {
                return;
            }
            if (generation.get() != expectedGen) {
                return;
            }
            int idx;
            for (;;) {
                idx = this.index.get();
                if (idx == 0 && !emitEmptyListIfNoItem) {
                    return;
                }
                if (this.index.compareAndSet(idx, 0)) {
                    break;
                }
            }
            if (generation.get() != expectedGen) {
                return;
            }
            flushCallback();
        }

        private void doOnSubscribe() {
            current = supplier.get();
            if (emitEmptyListIfNoItem) {
                scheduleFlush();
            }
        }

        void nextCallback(T value) {
            synchronized (this) {
                if (current == null) {
                    current = supplier.get();
                }
                current.add(value);
            }
        }

        private void flushCallback() {
            List<T> cur;
            boolean flush = false;
            synchronized (this) {
                if (current != null) {
                    cur = new ArrayList<>(current);
                } else {
                    cur = Collections.emptyList();
                }
                if (!cur.isEmpty() || emitEmptyListIfNoItem) {
                    current = supplier.get();
                    flush = true;
                }
            }

            if (flush) {
                long req = requested.get();
                MultiSubscriber<? super List<T>> subscriber = downstream;
                if (emitEmptyListIfNoItem && terminated.get() == RUNNING) {
                    scheduleFlush();
                }
                if (req != 0L) {

                    if (req != Long.MAX_VALUE) {
                        long next;
                        for (;;) {
                            next = req - 1;
                            if (requested.compareAndSet(req, next)) {
                                subscriber.onItem(cur);
                                return;
                            }

                            req = requested.get();
                            if (req <= 0L) {
                                break;
                            }
                        }
                    } else {
                        subscriber.onItem(cur);
                        return;
                    }
                }

                cancel();
                subscriber.onFailure(new BackPressureFailure("Cannot emit item due to lack of requests"));
            }
        }

        @Override
        public void onItem(final T value) {
            int index;
            boolean atBoundary;
            for (;;) {
                int current = this.index.get();
                index = current + 1;
                atBoundary = (index % size == 0);
                if (this.index.compareAndSet(current, atBoundary ? 0 : index)) {
                    break;
                }
            }

            if (index == 1 && !emitEmptyListIfNoItem) {
                scheduleFlush();
            }

            nextCallback(value);

            if (atBoundary) {
                generation.incrementAndGet();
                ScheduledFuture<?> t = task;
                if (t != null) {
                    t.cancel(false);
                    task = null;
                }
                flushCallback();
            }
        }

        void checkedComplete() {
            try {
                flushCallback();
            } finally {
                super.onCompletion();
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
                if (terminated.get() != RUNNING) {
                    return;
                }
                if (size == Integer.MAX_VALUE || n == Long.MAX_VALUE) {
                    super.request(Long.MAX_VALUE);
                } else {
                    super.request(Subscriptions.multiply(n, size));
                }
            } else {
                onFailure(Subscriptions.getInvalidRequestException());
            }
        }

        @Override
        public void onCompletion() {
            if (terminated.compareAndSet(RUNNING, SUCCEED)) {
                ScheduledFuture<?> t = task;
                if (t != null) {
                    t.cancel(false);
                    task = null;
                }
                checkedComplete();
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            if (terminated.compareAndSet(RUNNING, FAILED)) {
                synchronized (this) {
                    if (current != null) {
                        current.clear();
                        current = null;
                    }
                }
                super.onFailure(throwable);
            } else {
                Infrastructure.handleDroppedException(throwable);
            }
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                doOnSubscribe();
                downstream.onSubscribe(this);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void cancel() {
            if (terminated.compareAndSet(RUNNING, CANCELLED)) {
                ScheduledFuture<?> t = task;
                if (t != null) {
                    t.cancel(false);
                    task = null;
                }
                super.cancel();
                List<T> cur = current;
                if (cur != null) {
                    cur.clear();
                }
            }
        }
    }
}
