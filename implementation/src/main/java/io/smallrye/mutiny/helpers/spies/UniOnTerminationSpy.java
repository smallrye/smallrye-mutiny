package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;
import io.smallrye.mutiny.tuples.Tuple3;

public class UniOnTerminationSpy<T> extends UniSpyBase<T> {

    private volatile Tuple3<T, Throwable, Boolean> lastTermination;

    UniOnTerminationSpy(Uni<T> upstream) {
        super(upstream);
    }

    private void throwWhenTerminationIsNull() {
        if (lastTermination == null) {
            throw new IllegalStateException("The uni hasn't terminated yet ");
        }
    }

    public T lastTerminationItem() throws IllegalStateException {
        throwWhenTerminationIsNull();
        return lastTermination.getItem1();
    }

    public Throwable lastTerminationFailure() throws IllegalStateException {
        throwWhenTerminationIsNull();
        return lastTermination.getItem2();
    }

    public boolean lastTerminationWasCancelled() throws IllegalStateException {
        throwWhenTerminationIsNull();
        return lastTermination.getItem3();
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> downstream) {
        upstream()
                .onTermination().invoke((i, f, c) -> {
                    incrementInvocationCount();
                    lastTermination = Tuple3.of(i, f, c);
                })
                .subscribe().withSubscriber(downstream);
    }
}
