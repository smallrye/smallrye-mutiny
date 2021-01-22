package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniDelegatingSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniEmitOn<I> extends UniOperator<I, I> {
    private final Executor executor;

    public UniEmitOn(Uni<I> upstream, Executor executor) {
        super(upstream);
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSubscriber<? super I> subscriber) {
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
