package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.multi.MultiSignalConsumerOp;

public class MultiOnCompletionPeek<T> extends MultiOperator<T, T> {
    private final Runnable callback;

    public MultiOnCompletionPeek(Multi<T> upstream, Runnable callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    protected Publisher<T> publisher() {
        return new MultiSignalConsumerOp<>(
                upstream(),
                null,
                null,
                null,
                callback,
                null,
                null);
    }
}
