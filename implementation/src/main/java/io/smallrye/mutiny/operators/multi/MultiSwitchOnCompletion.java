package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.MultiOperator;

public class MultiSwitchOnCompletion<T> extends MultiOperator<T, T> {
    private final Supplier<Publisher<? extends T>> supplier;

    public MultiSwitchOnCompletion(Multi<T> upstream, Supplier<Publisher<? extends T>> supplier) {
        super(upstream);
        this.supplier = supplier;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        Publisher<T> followup = Multi.createFrom().deferred(() -> {
            Publisher<? extends T> publisher;
            try {
                publisher = supplier.get();
            } catch (Throwable e) {
                return Multi.createFrom().failure(e);
            }
            if (publisher == null) {
                return Multi.createFrom().failure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            }
            if (publisher instanceof Multi) {
                //noinspection unchecked
                return (Multi<T>) publisher;
            } else {
                return Multi.createFrom().publisher(publisher);
            }
        });

        @SuppressWarnings("unchecked")
        Publisher<T>[] publishers = new Publisher[] { upstream(), followup };
        Multi<T> op = Infrastructure.onMultiCreation(new MultiConcatOp<>(false, publishers));
        op.subscribe(subscriber);
    }
}
