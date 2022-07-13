package io.smallrye.mutiny.helpers.spies;

import java.util.concurrent.Flow.Subscription;

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
        upstream.onSubscription().invoke(subscription -> {
            incrementInvocationCount();
            lastSubscription = subscription;
        }).subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "MultiOnSubscribeSpy{" +
                "lastSubscription=" + lastSubscription +
                "} " + super.toString();
    }
}
