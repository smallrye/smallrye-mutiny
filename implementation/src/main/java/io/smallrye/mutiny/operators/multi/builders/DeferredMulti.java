package io.smallrye.mutiny.operators.multi.builders;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;

public class DeferredMulti<T> extends AbstractMulti<T> {
    private final Supplier<? extends Publisher<? extends T>> supplier;

    public DeferredMulti(Supplier<? extends Publisher<? extends T>> supplier) {
        this.supplier = ParameterValidation.nonNull(supplier, "supplier");
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        Publisher<? extends T> publisher;
        try {
            publisher = supplier.get();
            if (publisher == null) {
                throw new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL);
            }
        } catch (Throwable failure) {
            Subscriptions.fail(downstream, failure);
            return;
        }
        publisher.subscribe(downstream);
    }
}
