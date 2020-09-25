package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnCancellationSpy<T> extends MultiSpyBase<T> {

    MultiOnCancellationSpy(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.onCancellation().invoke(this::incrementInvocationCount)
                .subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "MultiOnCancellationSpy{} " + super.toString();
    }
}
