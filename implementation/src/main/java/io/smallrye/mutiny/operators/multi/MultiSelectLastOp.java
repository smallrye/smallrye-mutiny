package io.smallrye.mutiny.operators.multi;

import java.util.ArrayDeque;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Implementation of multi, caching and emitting the last n items from upstream (emitted before the upstream completion).
 *
 * @param <T> the type of item
 */
public class MultiSelectLastOp<T> extends AbstractMultiOperator<T, T> {

    private final int numberOfItems;

    public MultiSelectLastOp(Multi<? extends T> upstream, int numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        if (numberOfItems == 0) {
            upstream.subscribe(new TakeSelectLastZeroProcessor<>(subscriber));
        } else {
            upstream.subscribe(new MultiSelectLastProcessor<>(subscriber, numberOfItems));
        }
    }

    static final class TakeSelectLastZeroProcessor<T> extends MultiOperatorProcessor<T, T> {

        TakeSelectLastZeroProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                // Propagate subscription to downstream.
                downstream.onSubscribe(this);
                // Dropping all values.
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            // Do nothing, we are dropping all the values.
        }
    }

    static final class MultiSelectLastProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final int numberOfItems;
        private final ArrayDeque<T> queue;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        volatile boolean upstreamCompleted;

        MultiSelectLastProcessor(MultiSubscriber<? super T> downstream, int numberOfItems) {
            super(downstream);
            this.numberOfItems = numberOfItems;
            this.queue = new ArrayDeque<>(numberOfItems);
        }

        @Override
        public void request(long n) {
            if (n < 0) {
                onFailure(Subscriptions.getInvalidRequestException());
                return;
            }
            Subscriptions.add(requested, n);
            drain();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                // Propagate subscription to downstream.
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);

            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            if (queue.size() == numberOfItems) {
                queue.poll();
            }
            queue.offer(t);
        }

        @Override
        public void onCompletion() {
            upstreamCompleted = true;
            drain();
        }

        private void drain() {
            if (wip.getAndIncrement() == 0) {
                long req = requested.get();
                do {
                    if (getUpstreamSubscription() == Subscriptions.CANCELLED) {
                        return;
                    }
                    if (upstreamCompleted) {
                        long count = 0L;

                        while (count != req) {
                            if (getUpstreamSubscription() == Subscriptions.CANCELLED) {
                                return;
                            }
                            T item = queue.poll();
                            if (item == null) {
                                // No more items in the queue, completing.
                                downstream.onCompletion();
                                return;
                            }

                            downstream.onItem(item);
                            count++;
                        }

                        if (count != 0L && req != Long.MAX_VALUE) {
                            req = requested.addAndGet(-count);
                        }
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }

    }
}
