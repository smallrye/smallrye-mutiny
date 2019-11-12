package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.function.BiConsumer;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.multi.MultiSignalConsumerOp;

public class MultiOnTermination<T> extends MultiOperator<T, T> {
    private final BiConsumer<Throwable, Boolean> consumer;

    public MultiOnTermination(Multi<T> upstream, BiConsumer<Throwable, Boolean> consumer) {
        super(nonNull(upstream, "upstream"));
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected Publisher<T> publisher() {
        return new MultiSignalConsumerOp<>(
                upstream(),
                null,
                null,
                f -> consumer.accept(f, false),
                () -> consumer.accept(null, false),
                null,
                () -> consumer.accept(null, true));
    }
}
