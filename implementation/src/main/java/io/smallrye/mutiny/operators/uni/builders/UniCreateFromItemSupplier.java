package io.smallrye.mutiny.operators.uni.builders;

import java.util.function.Supplier;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the item is produced by a supplier.
 * The supplied item can be {@code null}.
 *
 * @param <T> the type of the item
 */
public class UniCreateFromItemSupplier<T> extends AbstractUni<T> {

    private final Supplier<? extends T> supplier;

    public UniCreateFromItemSupplier(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        // No need to track cancellation, it's done by the serialized subscriber downstream.
        subscriber.onSubscribe(EmptyUniSubscription.DONE);
        try {
            T item = supplier.get();
            subscriber.onItem(item);
        } catch (Throwable err) {
            subscriber.onFailure(err);
        }
    }
}
