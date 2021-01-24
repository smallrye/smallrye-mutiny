package io.smallrye.mutiny.operators.uni.builders;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

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
        subscriber.onSubscribe(EmptyUniSubscription.DONE);
        subscriber.onFailure(failure);
    }
}
