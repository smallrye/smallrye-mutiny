package io.smallrye.mutiny.helpers.test;

import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * A onSubscribe signal.
 */
public final class OnSubscribeUniSignal implements UniSignal {
    private final UniSubscription subscription;

    public OnSubscribeUniSignal(UniSubscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public UniSubscription value() {
        return subscription;
    }

    @Override
    public String toString() {
        return "OnSubscribeSignal{" +
                "subscription=" + subscription +
                '}';
    }
}
