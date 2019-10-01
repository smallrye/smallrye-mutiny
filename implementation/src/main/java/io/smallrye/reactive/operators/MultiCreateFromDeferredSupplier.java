package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

public class MultiCreateFromDeferredSupplier<T> extends MultiOperator<Void, T> {
    private final Supplier<? extends Multi<? extends T>> supplier;

    public MultiCreateFromDeferredSupplier(Supplier<? extends Multi<? extends T>> supplier) {
        super(null);
        this.supplier = nonNull(supplier, "supplier");
    }

    @Override
    protected Flowable<T> flowable() {
        return Flowable.defer(() -> {
            Multi<? extends T> multi = supplier.get();
            if (multi == null) {
                throw new NullPointerException(SUPPLIER_PRODUCED_NULL);
            }
            if (multi instanceof AbstractMulti) {
                //noinspection unchecked
                return ((AbstractMulti<? extends T>) multi).flowable();
            }
            return multi;
        });
    }
}
