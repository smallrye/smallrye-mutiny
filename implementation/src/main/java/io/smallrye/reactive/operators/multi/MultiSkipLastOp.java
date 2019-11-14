package io.smallrye.reactive.operators.multi;

import java.util.ArrayDeque;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;

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
    public void subscribe(Subscriber<? super T> actual) {
        if (numberOfItems == 0) {
            upstream.subscribe(actual);
        } else {
            upstream.subscribe(new SkipLastProcessor<>(actual, numberOfItems));
        }
    }

    static final class SkipLastProcessor<T>
            extends MultiOperatorProcessor<T, T> {

        private final int numberOfItems;
        private final ArrayDeque<T> queue = new ArrayDeque<>();

        SkipLastProcessor(Subscriber<? super T> actual, int numberOfItems) {
            super(actual);
            this.numberOfItems = numberOfItems;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                // Propagate subscription to downstream.
                downstream.onSubscribe(this);
                subscription.request(numberOfItems);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T t) {
            if (queue.size() == numberOfItems) {
                downstream.onNext(queue.pollFirst());
            }
            queue.offerLast(t);
        }

        @Override
        public void onError(Throwable t) {
            queue.clear();
            super.onError(t);
        }

        @Override
        public void onComplete() {
            queue.clear();
            super.onComplete();
        }

        @Override
        public void cancel() {
            super.cancel();
            queue.clear();
        }
    }
}
