package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnCancellationInvokeUni<I> extends UniOperator<I, I> {

    private final Supplier<Uni<?>> supplier;

    public UniOnCancellationInvokeUni(Uni<? extends I> upstream, Supplier<Uni<?>> supplier) {
        super(nonNull(upstream, "upstream"));
        this.supplier = nonNull(supplier, "supplier");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, I>(subscriber) {

            // Guard to invoke the supplier only once
            private final AtomicBoolean invoked = new AtomicBoolean();

            @Override
            public void onSubscribe(UniSubscription subscription) {
                subscriber.onSubscribe(new UniSubscription() {

                    @Override
                    public void cancel() {
                        execute().subscribe().with(
                                ignoredItem -> subscription.cancel(),
                                ignoredException -> subscription.cancel()); // TODO avoid swallowing this exception
                    }

                    private Uni<?> execute() {
                        if (invoked.compareAndSet(false, true)) {
                            try {
                                return nonNull(supplier.get(), "uni");
                            } catch (Throwable err) {
                                return Uni.createFrom().failure(err);
                            }
                        } else {
                            return Uni.createFrom().nullItem();
                        }
                    }
                });
            }
        });
    }
}
