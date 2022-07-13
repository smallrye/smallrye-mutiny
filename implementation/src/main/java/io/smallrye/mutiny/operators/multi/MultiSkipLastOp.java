package io.smallrye.mutiny.operators.multi;

import java.util.ArrayDeque;
import java.util.concurrent.Flow.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Skips the numberOfItems last items from upstream.
 *
 * @param <T> the type of item
 */
public final class MultiSkipLastOp<T> extends AbstractMultiOperator<T, T> {

    private final int numberOfItems;

    public MultiSkipLastOp(Multi<? extends T> upstream, int numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (numberOfItems == 0) {
            upstream.subscribe().withSubscriber(actual);
        } else {
            upstream.subscribe().withSubscriber(new SkipLastProcessor<>(actual, numberOfItems));
        }
    }

    static final class SkipLastProcessor<T>
            extends MultiOperatorProcessor<T, T> {

        private final int numberOfItems;
        private final ArrayDeque<T> queue = new ArrayDeque<>();

        SkipLastProcessor(MultiSubscriber<? super T> actual, int numberOfItems) {
            super(actual);
            this.numberOfItems = numberOfItems;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                // Propagate subscription to downstream.
                downstream.onSubscribe(this);
                subscription.request(numberOfItems);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            if (queue.size() == numberOfItems) {
                downstream.onItem(queue.pollFirst());
            }
            queue.offerLast(t);
        }

        @Override
        public void onFailure(Throwable t) {
            queue.clear();
            super.onFailure(t);
        }

        @Override
        public void onCompletion() {
            queue.clear();
            super.onCompletion();
        }

        @Override
        public void cancel() {
            super.cancel();
            queue.clear();
        }
    }
}
