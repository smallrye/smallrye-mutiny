package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.subscription.UniSubscription;

public class UniOnCancellation<T> extends UniOperator<T, T> {
    private final Runnable callback;

    public UniOnCancellation(Uni<T> upstream, Runnable callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                super.onSubscribe(() -> {
                    callback.run();
                    subscription.cancel();
                });
            }
        });
    }
}
