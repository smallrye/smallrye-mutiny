package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;

import io.smallrye.mutiny.subscription.UniEmitter;

public class UniCreateWithEmitter<T> extends AbstractUni<T> {
    private final Consumer<UniEmitter<? super T>> consumer;

    public UniCreateWithEmitter(Consumer<UniEmitter<? super T>> consumer) {
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        DefaultUniEmitter<? super T> emitter = new DefaultUniEmitter<>(subscriber);
        subscriber.onSubscribe(emitter);

        try {
            consumer.accept(emitter);
        } catch (RuntimeException e) {
            // we use the emitter to be sure that if the failure happens after the first event being fired, it
            // will be dropped.
            emitter.fail(e);
        }
    }
}
