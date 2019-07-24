package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

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
                throw new NullPointerException("The supplier returned `null`");
            }
            if (multi instanceof AbstractMulti) {
                //noinspection unchecked
                return ((AbstractMulti<? extends T>) multi).flowable();
            }
            return multi;
        });
    }
}
