package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnCompletionSpy<T> extends MultiSpyBase<T> {

    MultiOnCompletionSpy(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.onCompletion().invoke(this::incrementInvocationCount)
                .subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "MultiOnCompletionSpy{} " + super.toString();
    }
}
