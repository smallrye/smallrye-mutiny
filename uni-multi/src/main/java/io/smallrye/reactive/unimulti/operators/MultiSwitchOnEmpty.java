package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

public class MultiSwitchOnEmpty<T> extends MultiOperator<T, T> {
    private final Supplier<Publisher<? extends T>> supplier;

    public MultiSwitchOnEmpty(Multi<T> upstream, Supplier<Publisher<? extends T>> supplier) {
        super(upstream);
        this.supplier = supplier;
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().switchIfEmpty(Flowable.defer(() -> {
            Publisher<? extends T> publisher;
            try {
                publisher = supplier.get();
            } catch (Exception e) {
                return Flowable.error(e);
            }
            if (publisher == null) {
                return Flowable.error(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            }
            return publisher;
        }));
    }
}
