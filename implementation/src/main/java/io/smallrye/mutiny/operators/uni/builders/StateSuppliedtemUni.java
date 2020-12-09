package io.smallrye.mutiny.operators.uni.builders;

import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where items are from a supplier.
 * The supplied item can be {@code null}.
 *
 * @param <T> the type of the item
 * @param <S> the type of the state
 */
public class StateSuppliedtemUni<T, S> extends AbstractUni<T> {

    private final Supplier<S> stateSupplier;
    private final Function<S, ? extends T> mapper;
    private volatile boolean supplied = false;
    private volatile S state = null;

    public StateSuppliedtemUni(Supplier<S> stateSupplier, Function<S, ? extends T> mapper) {
        this.stateSupplier = stateSupplier;
        this.mapper = mapper;
    }

    @Override
    protected void subscribing(UniSubscriber<? super T> subscriber) {
        subscriber.onSubscribe(EmptyUniSubscription.CANCELLED);
        synchronized (this) {
            if (!supplied) {
                supplied = true;
                try {
                    this.state = stateSupplier.get();
                    if (this.state == null) {
                        subscriber.onFailure(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
                        return;
                    }
                } catch (Throwable err) {
                    subscriber.onFailure(err);
                    return;
                }
            }
        }
        if (this.state == null) {
            subscriber.onFailure(new IllegalStateException("Invalid shared state"));
        } else {
            try {
                subscriber.onItem(mapper.apply(this.state));
            } catch (Throwable err) {
                subscriber.onFailure(err);
            }
        }
    }
}
