package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.tuples.Tuple3;

public class UniOnTerminationSpy<T> extends UniSpyBase<T> {

    private volatile Tuple3<T, Throwable, Boolean> lastTermination;

    UniOnTerminationSpy(Uni<T> upstream) {
        super(upstream);
    }

    public T lastTerminationItem() throws IllegalStateException {
        return (lastTermination == null) ? null : lastTermination.getItem1();
    }

    public Throwable lastTerminationFailure() throws IllegalStateException {
        return (lastTermination == null) ? null : lastTermination.getItem2();
    }

    public boolean lastTerminationWasCancelled() throws IllegalStateException {
        return (lastTermination != null) && lastTermination.getItem3();
    }

    @Override
    public void reset() {
        super.reset();
        lastTermination = null;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> downstream) {
        upstream()
                .onTermination().invoke((i, f, c) -> {
                    incrementInvocationCount();
                    lastTermination = Tuple3.of(i, f, c);
                })
                .subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "UniOnTerminationSpy{" +
                "lastTermination=" + lastTermination +
                "} " + super.toString();
    }
}
