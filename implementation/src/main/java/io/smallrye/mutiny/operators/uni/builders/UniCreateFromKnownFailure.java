package io.smallrye.mutiny.operators.uni.builders;

import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the failure is known.
 * The failure cannot be {@code null}.
 *
 * @param <T> the type of the item
 */
public class UniCreateFromKnownFailure<T> extends AbstractUni<T> {

    private final Throwable failure;

    public UniCreateFromKnownFailure(Throwable failure) {
        this.failure = failure;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        new KnownFailureSubscription(subscriber).forward();
    }

    private class KnownFailureSubscription implements UniSubscription {

        private final UniSubscriber<? super T> subscriber;
        private volatile boolean cancelled = false;

        private KnownFailureSubscription(UniSubscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        private void forward() {
            subscriber.onSubscribe(this);
            if (!cancelled) {
                subscriber.onFailure(failure);
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
