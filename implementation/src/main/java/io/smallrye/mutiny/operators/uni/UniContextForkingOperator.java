package io.smallrye.mutiny.operators.uni;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

import java.util.function.Consumer;

public final class UniContextForkingOperator<T> extends UniOperator<T, T> {

    private final Consumer<Context> additionalSteps;

    public UniContextForkingOperator(Uni<? extends T> upstream, Consumer<Context> additionalSteps) {
        super(upstream);
        this.additionalSteps = additionalSteps;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniContextForkingOperatorProcessor<>(additionalSteps, subscriber));
    }

    private static class UniContextForkingOperatorProcessor<T> extends UniOperatorProcessor<T, T> {

        private final Context forkedContext;

        public UniContextForkingOperatorProcessor(Consumer<Context> additionalSteps, UniSubscriber<? super T> downstream) {
            super(downstream);
            this.forkedContext = downstream.context().fork();
            additionalSteps.accept(this.forkedContext);
        }

        @Override
        public Context context() {
            return forkedContext;
        }
    }
}
