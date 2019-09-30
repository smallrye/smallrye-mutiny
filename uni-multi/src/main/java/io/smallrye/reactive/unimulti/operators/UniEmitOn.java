package io.smallrye.reactive.unimulti.operators;

import io.smallrye.reactive.unimulti.Uni;

import java.util.concurrent.Executor;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

public class UniEmitOn<I> extends UniOperator<I, I> {
    private final Executor executor;

    UniEmitOn(Uni<I> upstream, Executor executor) {
        super(upstream);
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, I>(subscriber) {
            @Override
            public void onItem(I item) {
                executor.execute(() -> subscriber.onItem(item));
            }

            @Override public void onFailure(Throwable failure) {
                executor.execute(() -> subscriber.onFailure(failure));
            }
        });
    }
}
