

package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.subscription.BackPressureFailure;
import io.smallrye.reactive.subscription.SerializedSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

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

    public MultiBufferWithTimeoutOp(Multi<T> upstream,
            int size,
            Duration timeout,
            ScheduledExecutorService scheduler) {
        super(upstream);
        this.timeout = ParameterValidation.validate(timeout, "timeout");
        this.size = ParameterValidation.positive(size, "size");
        this.scheduler = ParameterValidation.nonNull(scheduler, "scheduler");
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
    public void subscribe(Subscriber<? super List<T>> downstream) {
        MultiBufferWithTimeoutProcessor<T> subscriber = new MultiBufferWithTimeoutProcessor<>(
                new SerializedSubscriber<>(downstream), size, timeout, scheduler, supplier);
        upstream.subscribe(subscriber);
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
        private final Runnable flush;

        private AtomicInteger terminated = new AtomicInteger(RUNNING);
        private AtomicLong requested = new AtomicLong();
        private AtomicInteger index = new AtomicInteger();
        private List<T> current;
        private ScheduledFuture<?> task;

        MultiBufferWithTimeoutProcessor(Subscriber<? super List<T>> downstream, int size, Duration timeout,
                ScheduledExecutorService executor, Supplier<List<T>> supplier) {
            super(downstream);
            this.duration = timeout;
            this.executor = executor;
            this.supplier = supplier;
            this.size = size;

            this.flush = () -> {
                if (terminated.get() == RUNNING) {
                    int index;
                    for (; ; ) {
                        index = this.index.get();
                        if (index == 0) {
                            return;
                        }
                        if (this.index.compareAndSet(index, 0)) {
                            break;
                        }
                    }
                    flushCallback();
                }
            };
        }

        private void doOnSubscribe() {
            current = supplier.get();
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
                if (!cur.isEmpty()) {
                    current = supplier.get();
                    flush = true;
                }
            }

            if (flush) {
                long req = requested.get();
                if (req != 0L) {
                    if (req != Long.MAX_VALUE) {
                        long next;
                        for (; ; ) {
                            next = req - 1;
                            if (requested.compareAndSet(req, next)) {
                                downstream.onNext(cur);
                                return;
                            }

                            req = requested.get();
                            if (req <= 0L) {
                                break;
                            }
                        }
                    } else {
                        downstream.onNext(cur);
                        return;
                    }
                }

                cancel();
                downstream.onError(new BackPressureFailure("Cannot emit item due to lack of requests"));
            }
        }

        @Override
        public void onNext(final T value) {
            int index;
            for (; ; ) {
                index = this.index.get() + 1;
                if (this.index.compareAndSet(index - 1, index)) {
                    break;
                }
            }

            if (index == 1) {
                try {
                    task = executor.schedule(flush, duration.toMillis(), TimeUnit.MILLISECONDS);
                } catch (RejectedExecutionException rejected) {
                    onError(rejected);
                    return;
                }
            }

            nextCallback(value);

            if (this.index.get() % size == 0) {
                this.index.lazySet(0);
                if (task != null) {
                    task.cancel(false);
                    task = null;
                }
                flushCallback();
            }
        }

        void checkedComplete() {
            try {
                flushCallback();
            } finally {
                super.onComplete();
            }
        }

        /**
         * @return has this {@link Subscriber} terminated with success ?
         */
        final boolean isCompleted() {
            return terminated.get() == SUCCEED;
        }

        /**
         * @return has this {@link Subscriber} terminated with an error ?
         */
        final boolean isFailed() {
            return terminated.get() == FAILED;
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
            }
        }

        @Override
        public void onComplete() {
            if (terminated.compareAndSet(RUNNING, SUCCEED)) {
                if (task != null) {
                    task.cancel(true);
                    task = null;
                }
                checkedComplete();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (terminated.compareAndSet(RUNNING, FAILED)) {
                synchronized (this) {
                    if (current != null) {
                        current.clear();
                        current = null;
                    }
                }
                super.onError(throwable);
            }
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                doOnSubscribe();
                downstream.onSubscribe(this);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void cancel() {
            if (terminated.compareAndSet(RUNNING, CANCELLED)) {
                super.cancel();
                List<T> cur = current;
                if (cur != null) {
                    cur.clear();
                }
            }
        }
    }
}

