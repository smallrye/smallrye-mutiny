package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnSubscribeSpy<T> extends UniSpyBase<T> {

    private volatile UniSubscription lastSubscription;

    public UniSubscription lastSubscription() {
        return lastSubscription;
    }

    UniOnSubscribeSpy(Uni<T> upstream) {
        super(upstream);
    }

    @Override
    public void reset() {
        super.reset();
        lastSubscription = null;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> downstream) {
        upstream()
                .onSubscribe().invoke(uniSubscription -> {
                    incrementInvocationCount();
                    lastSubscription = uniSubscription;
                })
                .subscribe().withSubscriber(downstream);
    }
}
