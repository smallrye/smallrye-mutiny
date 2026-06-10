package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniEmitOn<I> extends UniOperator<I, I> {
    private final Executor executor;

    public UniEmitOn(Uni<I> upstream, Executor executor) {
        super(upstream);
        this.executor = nonNull(executor, "executor");
    }

    @Override
    public void subscribe(UniSubscriber<? super I> subscriber) {
        AbstractUni.subscribe(upstream(), new UniEmitOnProcessor(subscriber));
    }

    private class UniEmitOnProcessor extends UniOperatorProcessor<I, I> {

        public UniEmitOnProcessor(UniSubscriber<? super I> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(I item) {
            if (!isCancelled()) {
                try {
                    executor.execute(() -> downstream.onItem(item));
                } catch (RejectedExecutionException e) {
                    downstream.onFailure(e);
                }
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isCancelled()) {
                try {
                    executor.execute(() -> downstream.onFailure(failure));
                } catch (RejectedExecutionException e) {
                    downstream.onFailure(new CompositeException(failure, e));
                }
            }
        }
    }
}
