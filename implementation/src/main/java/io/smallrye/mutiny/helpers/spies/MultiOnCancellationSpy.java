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

    public boolean isCancelled() {
        return invoked();
    }

    @Override
    public String toString() {
        return "MultiOnCancellationSpy{} " + super.toString();
    }

    public void assertCancelled() {
        if (!isCancelled()) {
            throw new AssertionError("Expected downstream cancellation, but it did not happen");
        }
    }

    public void assertNotCancelled() {
        if (isCancelled()) {
            throw new AssertionError("Did not expect to receive a downstream cancellation");
        }
    }
}
