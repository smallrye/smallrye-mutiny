package io.smallrye.mutiny.operators.uni.builders;

import java.util.function.Supplier;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the failure is produced by a supplier.
 * The supplied failure cannot be {@code null}.
 *
 * @param <T> the type of the item
 */
public class UniCreateFromFailureSupplier<T> extends AbstractUni<T> {

    private final Supplier<Throwable> supplier;

    public UniCreateFromFailureSupplier(Supplier<Throwable> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        // No need to track cancellation, it's done by the serialized subscriber downstream.
        subscriber.onSubscribe(EmptyUniSubscription.DONE);
        try {
            Throwable failure = supplier.get();
            if (failure == null) {
                subscriber.onFailure(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
            } else {
                subscriber.onFailure(failure);
            }
        } catch (Throwable err) {
            subscriber.onFailure(err);
        }
    }
}
