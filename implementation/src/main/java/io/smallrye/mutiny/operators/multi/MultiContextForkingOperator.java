package io.smallrye.mutiny.operators.multi;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

import java.util.function.Consumer;

public final class MultiContextForkingOperator<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<Context> additionalSteps;

    public MultiContextForkingOperator(Multi<? extends T> upstream, Consumer<Context> additionalSteps) {
        super(upstream);
        this.additionalSteps = additionalSteps;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        upstream.subscribe().withSubscriber(new MultiContextForkingOperatorProcessor<T>(subscriber, additionalSteps));
    }

    private static class MultiContextForkingOperatorProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Context forkedContext;

        public MultiContextForkingOperatorProcessor(MultiSubscriber<? super T> downstream, Consumer<Context> additionalSteps) {
            super(downstream);
            if (downstream instanceof ContextSupport provider) {
                this.forkedContext = provider.context().fork();
            } else {
                this.forkedContext = Context.empty();
            }
            additionalSteps.accept(this.forkedContext);
        }

        @Override
        public Context context() {
            return forkedContext;
        }
    }
}
