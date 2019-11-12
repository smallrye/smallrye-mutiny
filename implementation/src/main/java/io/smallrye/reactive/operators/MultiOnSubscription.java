package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.multi.MultiSignalConsumerOp;

public class MultiOnSubscription<T> extends MultiOperator<T, T> {
    private final Consumer<? super Subscription> consumer;

    public MultiOnSubscription(Multi<T> upstream, Consumer<? super Subscription> consumer) {
        super(nonNull(upstream, "upstream"));
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected Publisher<T> publisher() {
        return new MultiSignalConsumerOp<>(
                upstream(),
                consumer,
                null,
                null,
                null,
                null,
                null);
    }
}
