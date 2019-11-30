package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.MultiSwitchOnEmptyOp;
import io.smallrye.mutiny.operators.multi.builders.FailedMulti;

public class MultiSwitchOnEmpty<T> extends MultiOperator<T, T> {
    private final Supplier<Publisher<? extends T>> supplier;

    public MultiSwitchOnEmpty(Multi<T> upstream, Supplier<Publisher<? extends T>> supplier) {
        super(upstream);
        this.supplier = supplier;
    }

    @Override
    protected Publisher<T> publisher() {
        Supplier<Multi<? extends T>> actual = () -> {
            Publisher<? extends T> publisher;
            try {
                publisher = supplier.get();
            } catch (Exception e) {
                return new FailedMulti<>(e);
            }
            if (publisher == null) {
                return new FailedMulti<>(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            }
            if (publisher instanceof Multi) {
                //noinspection unchecked
                return (Multi) publisher;
            } else {
                return Multi.createFrom().publisher(publisher);
            }
        };
        Multi<? extends T> deferred = Multi.createFrom().deferred(actual);
        return new MultiSwitchOnEmptyOp<>(upstream(), deferred);
    }
}
