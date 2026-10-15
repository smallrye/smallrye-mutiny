
package io.smallrye.mutiny.operators.multi;

import java.time.Duration;
import java.util.ArrayDeque;
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

        private final ArrayDeque<List<T>> readyBatches = new ArrayDeque<>();
        private final AtomicInteger wip = new AtomicInteger();
        private volatile boolean upstreamDone;
        private volatile boolean completionSent;
        private final AtomicLong outstandingUpstream = new AtomicLong();

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
            int idx = this.index.get();
            do {
                if (idx == 0 && !emitEmptyListIfNoItem) {
                    return;
                }
                if (this.index.compareAndSet(idx, 0)) {
                    break;
                }
                idx = this.index.get();
            } while (true);
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
                if (size == Integer.MAX_VALUE) {
                    // Time-only grouping: upstream has unbounded demand, so we must enforce
                    // backpressure by failing when downstream demand is exhausted.
                    long req = requested.get();
                    MultiSubscriber<? super List<T>> subscriber = downstream;
                    if (emitEmptyListIfNoItem && terminated.get() == RUNNING) {
                        scheduleFlush();
                    }
                    if (req != 0L) {
                        if (req != Long.MAX_VALUE) {
                            do {
                                long next = req - 1L;
                                if (requested.compareAndSet(req, next)) {
                                    subscriber.onItem(cur);
                                    return;
                                }
                                req = requested.get();
                            } while (req > 0L);
                        } else {
                            subscriber.onItem(cur);
                            return;
                        }
                    }
                    if (terminated.get() != RUNNING) {
                        return;
                    }
                    cancel();
                    subscriber.onFailure(new BackPressureFailure("Cannot emit item due to lack of requests"));
                } else {
                    // Finite size: upstream demand is bounded, buffer until demand arrives.
                    synchronized (this) {
                        readyBatches.add(cur);
                    }
                    drain();
                    if (emitEmptyListIfNoItem && terminated.get() == RUNNING) {
                        scheduleFlush();
                    }
                }
            }
        }

        @Override
        public void onItem(final T value) {
            if (size != Integer.MAX_VALUE) {
                outstandingUpstream.decrementAndGet();
            }

            int index;
            boolean atBoundary;
            int current = this.index.get();
            do {
                index = current + 1;
                atBoundary = (index % size == 0);
                if (this.index.compareAndSet(current, atBoundary ? 0 : index)) {
                    break;
                }
                current = this.index.get();
            } while (true);

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
            List<T> cur;
            synchronized (this) {
                if (current != null) {
                    cur = new ArrayList<>(current);
                    current = null;
                } else {
                    cur = Collections.emptyList();
                }
                if (!cur.isEmpty() || emitEmptyListIfNoItem) {
                    readyBatches.add(cur);
                }
            }
            upstreamDone = true;
            drain();
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            do {
                long req = requested.get();
                while (req > 0L) {
                    List<T> batch;
                    synchronized (this) {
                        batch = readyBatches.poll();
                    }
                    if (batch == null) {
                        break;
                    }
                    if (req != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                    downstream.onItem(batch);
                    if (terminated.get() == RUNNING) {
                        requestUpstream();
                    }
                    req = requested.get();
                }

                if (upstreamDone && !completionSent) {
                    boolean empty;
                    synchronized (this) {
                        empty = readyBatches.isEmpty();
                    }
                    if (empty) {
                        completionSent = true;
                        super.onCompletion();
                        return;
                    }
                }
            } while (wip.addAndGet(-1) != 0);
        }

        private void requestUpstream() {
            if (size == Integer.MAX_VALUE || requested.get() <= 0) {
                return;
            }
            long outstanding = outstandingUpstream.get();
            long toRequest = Math.max(0, (long) size - outstanding);
            while (toRequest > 0) {
                if (outstandingUpstream.compareAndSet(outstanding, outstanding + toRequest)) {
                    super.request(toRequest);
                    return;
                }
                outstanding = outstandingUpstream.get();
                toRequest = Math.max(0, (long) size - outstanding);
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
                int state = terminated.get();
                if (state == CANCELLED || state == FAILED) {
                    return;
                }
                drain();
                if (state == RUNNING) {
                    if (size == Integer.MAX_VALUE || n == Long.MAX_VALUE) {
                        outstandingUpstream.set(Long.MAX_VALUE);
                        super.request(Long.MAX_VALUE);
                    } else {
                        requestUpstream();
                    }
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
                    readyBatches.clear();
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
                synchronized (this) {
                    if (current != null) {
                        current.clear();
                        current = null;
                    }
                    readyBatches.clear();
                }
            }
        }
    }
}
