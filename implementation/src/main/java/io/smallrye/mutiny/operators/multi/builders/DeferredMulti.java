package io.smallrye.mutiny.operators.multi.builders;

import java.util.concurrent.Flow;
import java.util.function.Supplier;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class DeferredMulti<T> extends AbstractMulti<T> {
    private final Supplier<? extends Flow.Publisher<? extends T>> supplier;

    public DeferredMulti(Supplier<? extends Flow.Publisher<? extends T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        Flow.Publisher<? extends T> publisher;
        try {
            publisher = supplier.get();
            if (publisher == null) {
                throw new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL);
            }
        } catch (Throwable failure) {
            Subscriptions.fail(downstream, failure);
            return;
        }
        publisher.subscribe(Infrastructure.onMultiSubscription(publisher, downstream));
    }
}
