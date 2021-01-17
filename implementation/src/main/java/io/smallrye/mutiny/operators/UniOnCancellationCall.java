package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnCancellationCall<I> extends UniOperator<I, I> {

    private final Supplier<Uni<?>> supplier;

    public UniOnCancellationCall(Uni<? extends I> upstream, Supplier<Uni<?>> supplier) {
        super(upstream);
        this.supplier = supplier;
    }

    @Override
    protected void subscribing(UniSubscriber<? super I> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, I>(subscriber) {

            @Override
            public void onSubscribe(UniSubscription subscription) {
                subscriber.onSubscribe(new UniSubscription() {

                    @Override
                    public void cancel() {
                        execute().subscribe().with(
                                ignoredItem -> subscription.cancel(),
                                ignoredException -> {
                                    Infrastructure.handleDroppedException(ignoredException);
                                    subscription.cancel();
                                });
                    }

                    private Uni<?> execute() {
                        try {
                            return nonNull(supplier.get(), "uni");
                        } catch (Throwable err) {
                            return Uni.createFrom().failure(err);
                        }
                    }
                });
            }
        });
    }
}
