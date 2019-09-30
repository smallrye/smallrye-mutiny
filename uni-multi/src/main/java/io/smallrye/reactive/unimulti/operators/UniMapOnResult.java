package io.smallrye.reactive.unimulti.operators;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.helpers.ParameterValidation;

import java.util.function.Function;

public class UniMapOnResult<I, O> extends UniOperator<I, O> {

    private final Function<? super I, ? extends O> mapper;

    public UniMapOnResult(Uni<I> source, Function<? super I, ? extends O> mapper) {
        super(ParameterValidation.nonNull(source, "source"));
        this.mapper = ParameterValidation.nonNull(mapper, "mapper");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, O>(subscriber) {

            @Override
            public void onItem(I item) {
                if (subscriber.isCancelledOrDone()) {
                    // Avoid calling the mapper if we are done to save some cycles.
                    // If the cancellation happen during the call, the events won't be dispatched.
                    return;
                }

                O outcome;
                try {
                    outcome = mapper.apply(item);
                    // We cannot call onItem here, as if onItem would throw an exception
                    // it would be caught and onFailure would be called. This would be illegal.
                } catch (Exception e) {
                    subscriber.onFailure(e);
                    return;
                }

                subscriber.onItem(outcome);
            }

        });
    }
}
