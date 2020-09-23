package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

import java.util.concurrent.atomic.AtomicLong;

public class UniOnSubscribeSpy<T> extends UniOperator<T, T> {

    private AtomicLong invocationCount = new AtomicLong();
    private volatile UniSubscription lastSubscription;

    public long invocationCount() {
        return invocationCount.get();
    }

    public boolean invoked() {
        return invocationCount() > 0;
    }

    public UniSubscription lastSubscription() {
        return lastSubscription;
    }

    UniOnSubscribeSpy(Uni<T> upstream) {
        super(upstream);
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> downstream) {
        upstream()
                .onSubscribe().invoke(uniSubscription -> {
                    invocationCount.incrementAndGet();
                    lastSubscription = uniSubscription;
                })
                .subscribe().withSubscriber(downstream);
    }
}
