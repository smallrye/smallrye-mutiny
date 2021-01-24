package io.smallrye.mutiny.operators.uni;

import java.util.function.BiFunction;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniOnItemOrFailureMap<I, O> extends UniOperator<I, O> {

    private final BiFunction<? super I, Throwable, ? extends O> mapper;

    public UniOnItemOrFailureMap(Uni<I> upstream, BiFunction<? super I, Throwable, ? extends O> mapper) {
        super(upstream);
        this.mapper = mapper;
    }

    @Override
    public void subscribe(UniSubscriber<? super O> downstream) {
        AbstractUni.subscribe(upstream(), new UniOnItemOrFailureMapProcessor(downstream));
    }

    private class UniOnItemOrFailureMapProcessor extends UniOperatorProcessor<I, O> {

        public UniOnItemOrFailureMapProcessor(UniSubscriber<? super O> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(I item) {
            if (!isCancelled()) {
                O outcome;
                try {
                    outcome = mapper.apply(item, null);
                    // We cannot call onItem here, as if onItem would throw an exception
                    // it would be caught and onFailure would be called. This would be illegal.
                } catch (Throwable e) { // NOSONAR
                    // Be sure to not call the mapper again with the failure.
                    downstream.onFailure(e);
                    return;
                }
                downstream.onItem(outcome);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isCancelled()) {
                O outcome;
                try {
                    outcome = mapper.apply(null, failure);
                    // We cannot call onItem here, as if onItem would throw an exception
                    // it would be caught and onFailure would be called. This would be illegal.
                } catch (Throwable e) { // NOSONAR
                    // Be sure to not call the mapper again with the failure.
                    downstream.onFailure(new CompositeException(failure, e));
                    return;
                }
                downstream.onItem(outcome);
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }
    }
}
