package io.smallrye.mutiny.helpers.spies;

import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnRequestSpy<T> extends MultiSpyBase<T> {

    private final AtomicLong requestedCount = new AtomicLong();

    public long requestedCount() {
        return requestedCount.get();
    }

    public MultiOnRequestSpy(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void reset() {
        super.reset();
        requestedCount.set(0);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.onRequest().invoke(count -> {
            incrementInvocationCount();
            Subscriptions.add(requestedCount, count);
        }).subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "MultiOnRequestSpy{" +
                "requestedCount=" + requestedCount +
                "} " + super.toString();
    }
}
