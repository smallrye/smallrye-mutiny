package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniCreateFromDeferredSupplierWithState<S, T> extends AbstractUni<T> {
    private final Function<S, Uni<? extends T>> mapper;
    private final StateHolder<S> holder;

    public UniCreateFromDeferredSupplierWithState(Supplier<S> stateSupplier, Function<S, Uni<? extends T>> mapper) {
        this.holder = new StateHolder<>(stateSupplier);
        this.mapper = mapper;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        nonNull(subscriber, "subscriber");

        S state;
        try {
            state = holder.get();
            // get() throws an NPE is the produced state is null.
        } catch (Exception e) {
            subscriber.onSubscribe(EmptyUniSubscription.CANCELLED);
            subscriber.onFailure(e);
            return;
        }

        Uni<? extends T> uni;
        try {
            uni = mapper.apply(state);
        } catch (Throwable e) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(e);
            return;
        }

        if (uni == null) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
        } else {
            AbstractUni.subscribe(uni, subscriber);
        }
    }
}
