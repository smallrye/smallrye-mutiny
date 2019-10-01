package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniSubscription;

import java.util.function.Consumer;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniOnSubscription<T> extends UniOperator<T, T> {
    private final Consumer<? super UniSubscription> consumer;

    public UniOnSubscription(Uni<T> upstream, Consumer<? super UniSubscription> consumer) {
        super(nonNull(upstream, "upstream"));
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                try {
                    consumer.accept(subscription);
                } catch (Exception e) {
                    subscriber.onSubscribe(CANCELLED);
                    subscriber.onFailure(e);
                    return;
                }
                subscriber.onSubscribe(subscription);
            }
        });
    }
}
