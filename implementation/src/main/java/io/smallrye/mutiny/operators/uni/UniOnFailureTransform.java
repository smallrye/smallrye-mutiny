package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniOnFailureTransform<I, O> extends UniOperator<I, O> {

    private final Function<? super Throwable, ? extends Throwable> mapper;
    private final Predicate<? super Throwable> predicate;

    public UniOnFailureTransform(Uni<I> upstream,
            Predicate<? super Throwable> predicate,
            Function<? super Throwable, ? extends Throwable> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
        this.predicate = nonNull(predicate, "predicate");
    }

    @Override
    public void subscribe(UniSubscriber<? super O> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnFailureTransformProcessor(subscriber));
    }

    private class UniOnFailureTransformProcessor extends UniOperatorProcessor<I, O> {

        public UniOnFailureTransformProcessor(UniSubscriber<? super O> downstream) {
            super(downstream);
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isCancelled()) {
                boolean test;
                try {
                    test = predicate.test(failure);
                } catch (Throwable err) {
                    downstream.onFailure(new CompositeException(failure, err));
                    return;
                }

                if (test) {
                    Throwable outcome;
                    try {
                        outcome = mapper.apply(failure);
                        // We cannot call onFailure here, as if onFailure would throw an exception
                        // it would be caught and onFailure would be called. This would be illegal.
                    } catch (Throwable e) {
                        downstream.onFailure(e);
                        return;
                    }
                    if (outcome == null) {
                        downstream.onFailure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
                    } else {
                        downstream.onFailure(outcome);
                    }
                } else {
                    downstream.onFailure(failure);
                }
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }
    }
}
