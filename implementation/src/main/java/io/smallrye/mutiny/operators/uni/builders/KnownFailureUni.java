package io.smallrye.mutiny.operators.uni.builders;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the failure is known.
 * The failure cannot be {@code null}.
 *
 * @param <T> the type of the item
 */
public class KnownFailureUni<T> extends AbstractUni<T> {

    private final Throwable failure;

    public KnownFailureUni(Throwable failure) {
        this.failure = failure;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        subscriber.onSubscribe(EmptyUniSubscription.CANCELLED);
        subscriber.onFailure(failure);
    }
}
