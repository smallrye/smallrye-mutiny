package io.smallrye.mutiny.operators.uni.builders;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the item is known.
 * The item can be {@code null}.
 *
 * @param <T> the type of the item
 */
public class UniCreateFromKnownItem<T> extends AbstractUni<T> {

    private final T item;

    public UniCreateFromKnownItem(T item) {
        this.item = item;
    }

    @Override
    protected void subscribing(UniSubscriber<? super T> subscriber) {
        // No need to track cancellation, it's done by the serialized subscriber downstream.
        subscriber.onSubscribe(EmptyUniSubscription.CANCELLED);
        subscriber.onItem(item);
    }
}
