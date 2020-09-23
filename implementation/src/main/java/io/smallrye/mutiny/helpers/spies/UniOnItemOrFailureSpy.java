package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;

public class UniOnItemOrFailureSpy<T> extends UniSpyBase<T> {

    private volatile T lastItem;
    private volatile Throwable lastFailure;

    UniOnItemOrFailureSpy(Uni<T> upstream) {
        super(upstream);
    }

    public boolean hasFailed() {
        return lastFailure != null;
    }

    public T lastItem() {
        return lastItem;
    }

    public Throwable lastFailure() {
        return lastFailure;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> downstream) {
        upstream()
                .onItemOrFailure().invoke((item, failure) -> {
                    synchronized (UniOnItemOrFailureSpy.this) {
                        lastItem = item;
                        lastFailure = failure;
                    }
                    incrementInvocationCount();
                })
                .subscribe().withSubscriber(downstream);
    }
}
