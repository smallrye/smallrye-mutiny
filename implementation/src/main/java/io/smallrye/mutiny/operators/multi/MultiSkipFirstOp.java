package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Skips the first N items from upstream.
 * Failures and completions are propagated.
 */
public final class MultiSkipFirstOp<T> extends AbstractMultiOperator<T, T> {

    private final long numberOfItems;

    public MultiSkipFirstOp(Multi<? extends T> upstream, long numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (numberOfItems == 0) {
            // Pass-through
            upstream.subscribe(actual);
        } else {
            upstream.subscribe(new SkipFirstProcessor<>(actual, numberOfItems));
        }
    }

    static final class SkipFirstProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final AtomicLong remaining;

        SkipFirstProcessor(MultiSubscriber<? super T> downstream, long items) {
            super(downstream);
            this.remaining = new AtomicLong(items);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                downstream.onSubscribe(this);
                long l = remaining.get();
                // Do not request 0
                if (l > 0) {
                    subscription.request(l);
                }
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
