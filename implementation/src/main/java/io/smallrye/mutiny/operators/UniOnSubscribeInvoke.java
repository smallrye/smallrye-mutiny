package io.smallrye.mutiny.operators;

import java.util.function.Consumer;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnSubscribeInvoke<T> extends UniOperator<T, T> {

    private final Consumer<? super UniSubscription> callback;

    public UniOnSubscribeInvoke(Uni<? extends T> upstream,
            Consumer<? super UniSubscription> callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    protected void subscribing(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Invoke callback
                try {
                    callback.accept(subscription);
                } catch (Throwable e) {
                    super.onSubscribe(EmptyUniSubscription.CANCELLED);
                    super.onFailure(e);
                    return;
                }

                // Pass the subscription downstream
                // Cannot be done in the try block as it may propagates 2 subscriptions.
                super.onSubscribe(subscription);
            }
        });
    }
}
