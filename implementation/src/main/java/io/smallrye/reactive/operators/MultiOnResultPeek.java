package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.multi.MultiSignalConsumerOp;
import org.reactivestreams.Publisher;

import java.util.function.Consumer;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiOnResultPeek<T> extends MultiOperator<T, T> {
    private final Consumer<? super T> callback;

    public MultiOnResultPeek(Multi<T> upstream, Consumer<? super T> callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    protected Publisher<T> publisher() {
        return new MultiSignalConsumerOp<>(
                upstream(),
                null,
                callback,
                null,
                null,
                null,
                null
        );
    }
}
