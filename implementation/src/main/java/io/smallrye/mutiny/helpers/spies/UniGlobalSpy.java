package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniGlobalSpy<T> extends UniSpyBase<T> {

    private final UniOnCancellationSpy<T> onCancellationSpy;
    private final UniOnFailureSpy<T> onFailureSpy;
    private final UniOnItemOrFailureSpy<T> onItemOrFailureSpy;
    private final UniOnItemSpy<T> onItemSpy;
    private final UniOnSubscribeSpy<T> onSubscribeSpy;
    private final UniOnTerminationSpy<T> onTerminationSpy;

    UniGlobalSpy(Uni<T> upstream) {
        super(upstream);
        onCancellationSpy = Spy.onCancellation(upstream);
        onFailureSpy = Spy.onFailure(onCancellationSpy);
        onItemOrFailureSpy = Spy.onItemOrFailure(onFailureSpy);
        onItemSpy = Spy.onItem(onItemOrFailureSpy);
        onSubscribeSpy = Spy.onSubscribe(onItemSpy);
        onTerminationSpy = Spy.onTermination(onSubscribeSpy);
    }

    public UniOnCancellationSpy<T> onCancellationSpy() {
        return onCancellationSpy;
    }

    public UniOnFailureSpy<T> onFailureSpy() {
        return onFailureSpy;
    }

    public UniOnItemOrFailureSpy<T> onItemOrFailureSpy() {
        return onItemOrFailureSpy;
    }

    public UniOnItemSpy<T> onItemSpy() {
        return onItemSpy;
    }

    public UniOnSubscribeSpy<T> onSubscribeSpy() {
        return onSubscribeSpy;
    }

    public UniOnTerminationSpy<T> onTerminationSpy() {
        return onTerminationSpy;
    }

    @Override
    public long invocationCount() {
        return onCancellationSpy.invocationCount() + onFailureSpy.invocationCount() + onItemSpy.invocationCount()
                + onItemOrFailureSpy.invocationCount() + onSubscribeSpy.invocationCount() + onTerminationSpy.invocationCount();
    }

    @Override
    public boolean invoked() {
        return invocationCount() > 0;
    }

    @Override
    public void reset() {
        onCancellationSpy.reset();
        onFailureSpy.reset();
        onItemSpy.reset();
        onItemOrFailureSpy.reset();
        onSubscribeSpy.reset();
        onTerminationSpy.reset();
    }

    @Override
    public void subscribe(UniSubscriber<? super T> downstream) {
        onTerminationSpy.subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "UniGlobalSpy{" +
                "onCancellationSpy=" + onCancellationSpy +
                ", onFailureSpy=" + onFailureSpy +
                ", onItemOrFailureSpy=" + onItemOrFailureSpy +
                ", onItemSpy=" + onItemSpy +
                ", onSubscribeSpy=" + onSubscribeSpy +
                ", onTerminationSpy=" + onTerminationSpy +
                "} " + super.toString();
    }
}
