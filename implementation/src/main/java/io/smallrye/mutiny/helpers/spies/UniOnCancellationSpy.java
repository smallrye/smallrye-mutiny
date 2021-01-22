package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniOnCancellationSpy<T> extends UniSpyBase<T> {

    UniOnCancellationSpy(Uni<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(UniSubscriber<? super T> downstream) {
        upstream()
                .onCancellation().invoke(this::incrementInvocationCount)
                .subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "UniOnCancellationSpy{} " + super.toString();
    }
}
