package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;

public class UniOnCancellationSpy<T> extends UniSpyBase<T> {

    UniOnCancellationSpy(Uni<T> upstream) {
        super(upstream);
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> downstream) {
        upstream()
                .onCancellation().invoke(this::incrementInvocationCount)
                .subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "UniOnCancellationSpy{} " + super.toString();
    }
}
