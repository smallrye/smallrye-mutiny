package io.smallrye.mutiny.helpers.spies;

import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniOperator;

abstract class UniSpyBase<T> extends UniOperator<T, T> {

    private final AtomicLong invocationCount = new AtomicLong();

    protected void incrementInvocationCount() {
        invocationCount.incrementAndGet();
    }

    public long invocationCount() {
        return invocationCount.get();
    }

    public boolean invoked() {
        return invocationCount() > 0;
    }

    UniSpyBase(Uni<T> upstream) {
        super(upstream);
    }

    public void reset() {
        invocationCount.set(0);
    }

    @Override
    public String toString() {
        return "UniSpyBase{" +
                "invocationCount=" + invocationCount +
                "}";
    }
}