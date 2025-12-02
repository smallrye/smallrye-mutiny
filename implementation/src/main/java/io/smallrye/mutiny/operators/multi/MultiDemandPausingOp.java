package io.smallrye.mutiny.operators.multi;

import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.operators.MultiOperator;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.PausableMulti;

/**
 * Operator that allows pausing and resuming demand propagation to upstream.
 * <p>
 * When paused, this operator stops requesting new items from upstream.
 * Already-requested items are handled according to the configured {@link BackPressureStrategy}:
 * <ul>
 * <li>{@link BackPressureStrategy#BUFFER}: Items are buffered and delivered when resumed</li>
 * <li>{@link BackPressureStrategy#DROP}: Items are dropped</li>
 * <li>{@link BackPressureStrategy#IGNORE}: Items continue to flow downstream</li>
 * </ul>
 *
 * @param <T> the type of items
 */
public class MultiDemandPausingOp<T> extends MultiOperator<T, T> implements PausableMulti {

    private volatile PausableProcessor processor;

    private final AtomicBoolean paused;
    private final AtomicBoolean subscribed = new AtomicBoolean();
    private final boolean lateSubscription;
    private final int bufferSize;
    private final boolean unbounded;
    private final BackPressureStrategy backPressureStrategy;

    public MultiDemandPausingOp(Multi<T> upstream, boolean initiallyPaused, boolean lateSubscription, int bufferSize,
            boolean unbounded, BackPressureStrategy backPressureStrategy) {
        super(upstream);
        this.paused = new AtomicBoolean(initiallyPaused);
        this.lateSubscription = lateSubscription;
        this.bufferSize = bufferSize;
        this.unbounded = unbounded;
        this.backPressureStrategy = backPressureStrategy;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        processor = new PausableProcessor(subscriber);
        if (!lateSubscription || !paused.get()) { // if late subscription is disabled, we can subscribe now.
            subscribed.set(true);
            upstream().subscribe(processor);
        }
    }

    @Override
    public boolean isPaused() {
        return paused.get();
    }

    @Override
    public void pause() {
        paused.set(true);
    }

    @Override
    public void resume() {
        if (paused.compareAndSet(true, false)) {
            PausableProcessor p = processor;
            if (p != null) {
                if (lateSubscription && subscribed.compareAndSet(false, true)) {
                    upstream().subscribe(p);
                }
                p.resume();
            }
        }
    }

    @Override
    public int bufferSize() {
        PausableProcessor p = processor;
        if (p != null) {
            return p.queueSize();
        }
        return 0;
    }

    @Override
    public boolean clearBuffer() {
        if (paused.get()) {
            PausableProcessor p = processor;
            if (p != null) {
                p.clearQueue();
                return true;
            }
        }
        return false;
    }

    private class PausableProcessor extends MultiOperatorProcessor<T, T> {

        private final AtomicLong demand = new AtomicLong();
        private final Queue<T> queue;
        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicInteger strictBoundCounter = new AtomicInteger(0);
        private volatile boolean upstreamCompleted;
        private final AtomicBoolean clearQueue = new AtomicBoolean();

        PausableProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
            // Determine if we need a queue based on strategy and buffer size
            if (backPressureStrategy == BackPressureStrategy.BUFFER) {
                this.queue = unbounded ? Queues.<T> unbounded(bufferSize).get() : Queues.<T> get(bufferSize).get();
            } else {
                this.queue = null;
            }
        }

        void resume() {
            Flow.Subscription subscription = getUpstreamSubscription();
            if (subscription == Subscriptions.CANCELLED) {
                return;
            }
            // Drain any buffered items first
            drain();
            long currentDemand = demand.get();
            if (currentDemand > 0) {
                Subscriptions.produced(demand, currentDemand);
                subscription.request(currentDemand);
            }
        }

        void drain() {
            if (queue == null) {
                if (upstreamCompleted) {
                    super.onCompletion();
                }
                return;
            }
            if (wip.getAndIncrement() > 0) {
                return;
            }
            while (true) {
                Queue<T> qe = queue;
                // Drain all buffered items - these were already requested from upstream
                // so we don't need to check downstream demand here
                while (!paused.get()) {
                    T item = qe.poll();
                    if (item == null) {
                        // queue empty
                        break;
                    }
                    if (!unbounded) {
                        strictBoundCounter.decrementAndGet();
                    }
                    if (clearQueue.get()) {
                        break;
                    }
                    downstream.onItem(item);
                }
                if (!paused.get() && upstreamCompleted) {
                    super.onCompletion();
                }
                if (clearQueue.compareAndSet(true, false)) {
                    queue.clear();
                    strictBoundCounter.set(0);
                }
                if (wip.decrementAndGet() == 0) {
                    return;
                }
            }
        }

        void clearQueue() {
            if (queue != null && clearQueue.compareAndSet(false, true) && wip.getAndIncrement() == 0) {
                // nothing was currently dispatched, clearing the queue.
                queue.clear();
                clearQueue.set(false);
                strictBoundCounter.set(0);
                wip.decrementAndGet();
            }
        }

        int queueSize() {
            return (queue != null) ? queue.size() : 0;
        }

        @Override
        public void onItem(T item) {
            if (backPressureStrategy != BackPressureStrategy.IGNORE && paused.get()) {
                if (backPressureStrategy == BackPressureStrategy.DROP) {
                    return;
                }
                // When paused buffer items if necessary
                if ((!unbounded && strictBoundCounter.getAndIncrement() >= bufferSize) || !queue.offer(item)) {
                    // Buffer is full, throw exception
                    onFailure(new IllegalStateException("Buffer overflow: cannot buffer more than " + bufferSize + " items"));
                }
            } else {
                super.onItem(item);
            }
        }

        @Override
        public void request(long numberOfItems) {
            if (numberOfItems <= 0) {
                onFailure(Subscriptions.getInvalidRequestException());
                return;
            }
            Flow.Subscription subscription = getUpstreamSubscription();
            if (subscription == Subscriptions.CANCELLED) {
                return;
            }
            try {
                Subscriptions.add(demand, numberOfItems);
                if (paused.get()) {
                    return;
                }
                long currentDemand = demand.get();
                if (currentDemand > 0) {
                    Subscriptions.produced(demand, currentDemand);
                    subscription.request(currentDemand);
                }
            } catch (Throwable failure) {
                onFailure(failure);
            }
        }

        @Override
        public void cancel() {
            clearQueue();
            processor = null;
            super.cancel();
        }

        @Override
        public void onFailure(Throwable failure) {
            clearQueue();
            super.onFailure(failure);
        }

        @Override
        public void onCompletion() {
            upstreamCompleted = true;
            drain();
        }
    }
}
