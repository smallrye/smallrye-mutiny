package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;

import java.util.concurrent.Executor;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniHandleFailureOn<I> extends UniOperator<I, I> {
    private final Executor executor;

    UniHandleFailureOn(Uni<I> upstream, Executor executor) {
        super(upstream);
        this.executor = nonNull(executor, "executor");
    }

    @Override
    public void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, I>(subscriber) {

            @Override
            public void onFailure(Throwable throwable) {
                executor.execute(() -> subscriber.onFailure(throwable));
            }
        });
    }
}
