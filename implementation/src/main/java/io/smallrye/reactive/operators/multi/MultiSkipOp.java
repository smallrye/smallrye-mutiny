package io.smallrye.reactive.operators.multi;

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;

/**
 * Skips the first N items from upstream.
 * Failures and completations are propagated.
 */
public final class MultiSkipOp<T> extends AbstractMultiWithUpstream<T, T> {

    final long numberOfItems;

    public MultiSkipOp(Multi<? extends T> upstream, long numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        if (numberOfItems == 0) {
            upstream.subscribe(actual);
        } else {
            upstream.subscribe(new SkipSubscriber<>(actual, numberOfItems));
        }
    }

    static final class SkipSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final AtomicLong remaining;

        SkipSubscriber(Subscriber<? super T> downstream, long items) {
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
        public void onNext(T t) {
            long r = remaining.getAndDecrement();
            if (r <= 0L) {
                downstream.onNext(t);
            }
            // Other elements are skipped.
        }
    }
}
