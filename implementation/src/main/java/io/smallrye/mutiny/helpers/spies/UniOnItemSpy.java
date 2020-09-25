package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;

public class UniOnItemSpy<T> extends UniSpyBase<T> {

    private volatile T lastItem;

    UniOnItemSpy(Uni<T> upstream) {
        super(upstream);
    }

    public T lastItem() {
        return lastItem;
    }

    @Override
    public void reset() {
        super.reset();
        lastItem = null;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> downstream) {
        upstream()
                .onItem().invoke(item -> {
                    this.lastItem = item;
                    incrementInvocationCount();
                })
                .subscribe().withSubscriber(downstream);
    }
}
