package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.helpers.ParameterValidation;

public class UniCreateFromDeferredSupplier<T> extends UniOperator<Void, T> {
    private final Supplier<? extends Uni<? extends T>> supplier;

    public UniCreateFromDeferredSupplier(Supplier<? extends Uni<? extends T>> supplier) {
        super(null);
        this.supplier = nonNull(supplier, "supplier");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
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
            subscriber.onFailure(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
        } else {
            uni.subscribe().withSubscriber(subscriber);
        }
    }
}
