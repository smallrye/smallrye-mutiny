package io.smallrye.mutiny.operators.uni;

import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniOnItemTransform<I, O> extends UniOperator<I, O> {

    private final Function<? super I, ? extends O> mapper;

    public UniOnItemTransform(Uni<I> source, Function<? super I, ? extends O> mapper) {
        super(ParameterValidation.nonNull(source, "source"));
        this.mapper = mapper;
    }

    @Override
    public void subscribe(UniSubscriber<? super O> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnItemTransformProcessor(subscriber));
    }

    private class UniOnItemTransformProcessor extends UniOperatorProcessor<I, O> {

        public UniOnItemTransformProcessor(UniSubscriber<? super O> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(I item) {
            if (!isCancelled()) {
                O outcome;
                try {
                    outcome = mapper.apply(item);
                    // We cannot call onItem here, as if onItem would throw an exception
                    // it would be caught and onFailure would be called. This would be illegal.
                } catch (Throwable e) {
                    downstream.onFailure(e);
                    return;
                }
                downstream.onItem(outcome);
            }
        }
    }
}
