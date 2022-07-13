package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.MultiOperator;
import io.smallrye.mutiny.operators.multi.builders.FailedMulti;

public class MultiSwitchOnEmpty<T> extends MultiOperator<T, T> {
    private final Supplier<Publisher<? extends T>> supplier;

    public MultiSwitchOnEmpty(Multi<T> upstream, Supplier<Publisher<? extends T>> supplier) {
        super(upstream);
        this.supplier = supplier;
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        if (downstream == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        Supplier<Multi<? extends T>> actual = () -> {
            Publisher<? extends T> publisher;
            try {
                publisher = supplier.get();
            } catch (Throwable e) {
                return Infrastructure.onMultiCreation(new FailedMulti<>(e));
            }
            if (publisher == null) {
                return Infrastructure.onMultiCreation(new FailedMulti<>(new NullPointerException(SUPPLIER_PRODUCED_NULL)));
            }
            if (publisher instanceof Multi) {
                //noinspection unchecked
                return (Multi<? extends T>) publisher;
            } else {
                return Multi.createFrom().publisher(publisher);
            }
        };

        Multi<? extends T> deferred = Multi.createFrom().deferred(actual);
        Multi<T> op = Infrastructure.onMultiCreation(new MultiSwitchOnEmptyOp<>(upstream(), deferred));
        op.subscribe(downstream);
    }
}
