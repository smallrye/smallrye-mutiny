package io.smallrye.reactive.operators;

import io.smallrye.reactive.subscription.UniEmitter;

import java.util.function.Consumer;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniCreateWithEmitter<T> extends AbstractUni<T> {
    private final Consumer<UniEmitter<? super T>> consumer;

    public UniCreateWithEmitter(Consumer<UniEmitter<? super T>> consumer) {
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    public void subscribing(UniSerializedSubscriber<? super T> subscriber) {
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
