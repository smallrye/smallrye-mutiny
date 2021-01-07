package io.smallrye.mutiny.operators.uni.builders;

import java.util.Objects;
import java.util.function.Supplier;

import io.smallrye.mutiny.helpers.ParameterValidation;

public class StateHolder<S> {

    private final Supplier<S> supplier;
    private boolean once = false;
    private volatile S state;

    public StateHolder(Supplier<S> supplier) {
        this.supplier = supplier;
    }

    public S get() {
        synchronized (this) {
            if (!once) {
                this.once = true;
                this.state = supplier.get();
                Objects.requireNonNull(state, ParameterValidation.SUPPLIER_PRODUCED_NULL);
            }
        }
        if (state == null) {
            throw new IllegalStateException("Invalid shared state");
        }
        return state;
    }

}
