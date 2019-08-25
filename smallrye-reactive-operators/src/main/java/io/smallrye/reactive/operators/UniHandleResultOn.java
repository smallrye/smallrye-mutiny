package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;

import java.util.concurrent.Executor;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniHandleResultOn<I> extends UniOperator<I, I> {
    private final Executor executor;

    UniHandleResultOn(Uni<I> upstream, Executor executor) {
        super(upstream);
        this.executor = nonNull(executor, "executor");
    }

    @Override
    public void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, I>(subscriber) {
            @Override
            public void onItem(I item) {
                executor.execute(() -> subscriber.onItem(item));
            }
        });
    }
}
