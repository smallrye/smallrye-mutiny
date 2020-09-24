package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.tuples.Tuple2;

public class MultiOnTerminationSpy<T> extends MultiSpyBase<T> {

    private volatile Tuple2<Throwable, Boolean> lastTermination;

    private void throwWhenTerminationIsNull() {
        if (lastTermination == null) {
            throw new IllegalStateException("The uni hasn't terminated yet ");
        }
    }

    public Throwable lastTerminationFailure() throws IllegalStateException {
        throwWhenTerminationIsNull();
        return lastTermination.getItem1();
    }

    public boolean lastTerminationWasCancelled() throws IllegalStateException {
        throwWhenTerminationIsNull();
        return lastTermination.getItem2();
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
}
