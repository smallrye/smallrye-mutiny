package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.multi.MultiConcatOp;

public class MultiSwitchOnCompletion<T> extends MultiOperator<T, T> {
    private final Supplier<Publisher<? extends T>> supplier;

    public MultiSwitchOnCompletion(Multi<T> upstream, Supplier<Publisher<? extends T>> supplier) {
        super(upstream);
        this.supplier = supplier;
    }

    @Override
    protected Publisher<T> publisher() {
        Publisher<T> followup = Multi.createFrom().deferred(() -> {
            Publisher<? extends T> publisher;
            try {
                publisher = supplier.get();
            } catch (Exception e) {
                return Multi.createFrom().failure(e);
            }
            if (publisher == null) {
                return Multi.createFrom().failure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            }
            if (publisher instanceof Multi) {
                return (Multi) publisher;
            } else {
                return Multi.createFrom().publisher(publisher);
            }
        });

        @SuppressWarnings("unchecked")
        Publisher<T>[] publishers = new Publisher[] { upstream(), followup };
        return new MultiConcatOp<>(false, publishers);
    }
}
