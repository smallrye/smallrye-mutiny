package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;

import io.smallrye.mutiny.Uni;

public class UniEmitOn<I> extends UniOperator<I, I> {
    private final Executor executor;

    UniEmitOn(Uni<I> upstream, Executor executor) {
        super(upstream);
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, I>(subscriber) {
            @Override
            public void onItem(I item) {
                executor.execute(() -> subscriber.onItem(item));
            }

            @Override
            public void onFailure(Throwable failure) {
                executor.execute(() -> subscriber.onFailure(failure));
            }
        });
    }
}
