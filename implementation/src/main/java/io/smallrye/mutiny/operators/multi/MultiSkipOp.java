package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Skips the first N items from upstream.
 * Failures and completions are propagated.
 */
public final class MultiSkipOp<T> extends AbstractMultiOperator<T, T> {

    private final long numberOfItems;

    public MultiSkipOp(Multi<? extends T> upstream, long numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (numberOfItems == 0) {
            upstream.subscribe().withSubscriber(actual);
        } else {
            upstream.subscribe().withSubscriber(new SkipProcessor<>(actual, numberOfItems));
        }
    }

    static final class SkipProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final AtomicLong remaining;

        SkipProcessor(MultiSubscriber<? super T> downstream, long items) {
            super(downstream);
            this.remaining = new AtomicLong(items);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(remaining.get());
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            long r = remaining.getAndDecrement();
            if (r <= 0L) {
                downstream.onItem(t);
            }
            // Other elements are skipped.
        }
    }
}
