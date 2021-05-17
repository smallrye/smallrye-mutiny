package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
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
    public void subscribe(UniSubscriber<? super T> downstream) {
        upstream()
                .onSubscription().invoke(uniSubscription -> {
                    incrementInvocationCount();
                    lastSubscription = uniSubscription;
                })
                .subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "UniOnSubscribeSpy{" +
                "lastSubscription=" + lastSubscription +
                "} " + super.toString();
    }
}
