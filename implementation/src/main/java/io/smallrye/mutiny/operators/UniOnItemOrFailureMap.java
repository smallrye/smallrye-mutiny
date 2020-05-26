package io.smallrye.mutiny.operators;

import java.util.function.BiFunction;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;

public class UniOnItemOrFailureMap<I, O> extends UniOperator<I, O> {

    private final BiFunction<? super I, Throwable, ? extends O> mapper;

    public UniOnItemOrFailureMap(Uni<I> source, BiFunction<? super I, Throwable, ? extends O> mapper) {
        super(ParameterValidation.nonNull(source, "source"));
        this.mapper = ParameterValidation.nonNull(mapper, "mapper");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, O>(subscriber) {

            @Override
            public void onItem(I item) {
                if (!subscriber.isCancelledOrDone()) {
                    O outcome;
                    try {
                        outcome = mapper.apply(item, null);
                        // We cannot call onItem here, as if onItem would throw an exception
                        // it would be caught and onFailure would be called. This would be illegal.
                    } catch (Throwable e) { // NOSONAR
                        // Be sure to not call the mapper again with the failure.
                        subscriber.onFailure(e);
                        return;
                    }

                    subscriber.onItem(outcome);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (!subscriber.isCancelledOrDone()) {
                    O outcome;
                    try {
                        outcome = mapper.apply(null, failure);
                        // We cannot call onItem here, as if onItem would throw an exception
                        // it would be caught and onFailure would be called. This would be illegal.
                    } catch (Throwable e) { // NOSONAR
                        // Be sure to not call the mapper again with the failure.
                        subscriber.onFailure(new CompositeException(failure, e));
                        return;
                    }

                    subscriber.onItem(outcome);
                }
            }
        });
    }
}
