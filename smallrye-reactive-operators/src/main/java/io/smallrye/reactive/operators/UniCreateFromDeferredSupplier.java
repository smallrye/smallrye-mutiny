package io.smallrye.reactive.operators;


import io.smallrye.reactive.Uni;

import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;


public class UniCreateFromDeferredSupplier<T> extends UniOperator<Void, T> {
    private final Supplier<? extends Uni<? extends T>> supplier;

    public UniCreateFromDeferredSupplier(Supplier<? extends Uni<? extends T>> supplier) {
        super(null);
        this.supplier = nonNull(supplier, "supplier");
    }

    @Override
    public void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        nonNull(subscriber, "subscriber");
        Uni<? extends T> uni;
        try {
            uni = supplier.get();
        } catch (Exception e) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(e);
            return;
        }

        if (uni == null) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(new NullPointerException("The supplier produced `null`"));
        } else {
            uni.subscribe().withSubscriber(subscriber);
        }
    }
}
