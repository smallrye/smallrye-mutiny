package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.tuples.Tuple2;

public class MultiOnTerminationSpy<T> extends MultiSpyBase<T> {

    private volatile Tuple2<Throwable, Boolean> lastTermination;

    public Throwable lastTerminationFailure() throws IllegalStateException {
        return (lastTermination == null) ? null : lastTermination.getItem1();
    }

    public boolean lastTerminationWasCancelled() throws IllegalStateException {
        return (lastTermination != null) && lastTermination.getItem2();
    }

    @Override
    public void reset() {
        super.reset();
        lastTermination = null;
    }

    MultiOnTerminationSpy(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.onTermination().invoke((failure, cancelled) -> {
            lastTermination = Tuple2.of(failure, cancelled);
            incrementInvocationCount();
        }).subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "MultiOnTerminationSpy{" +
                "lastTermination=" + lastTermination +
                "} " + super.toString();
    }
}
