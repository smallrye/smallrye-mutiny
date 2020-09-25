package io.smallrye.mutiny.helpers.spies;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnSubscribeSpy<T> extends MultiSpyBase<T> {

    private volatile Subscription lastSubscription;

    public Subscription lastSubscription() {
        return lastSubscription;
    }

    MultiOnSubscribeSpy(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void reset() {
        super.reset();
        lastSubscription = null;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.onSubscribe().invoke(subscription -> {
            incrementInvocationCount();
            lastSubscription = subscription;
        }).subscribe().withSubscriber(downstream);
    }
}
