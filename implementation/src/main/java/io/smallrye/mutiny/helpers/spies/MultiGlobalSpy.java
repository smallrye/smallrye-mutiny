package io.smallrye.mutiny.helpers.spies;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiGlobalSpy<T> extends MultiSpyBase<T> {

    private final MultiOnCancellationSpy<T> onCancellationSpy;
    private final MultiOnCompletionSpy<T> onCompletionSpy;
    private final MultiOnFailureSpy<T> onFailureSpy;
    private final MultiOnItemSpy<T> onItemSpy;
    private final MultiOnRequestSpy<T> onRequestSpy;
    private final MultiOnSubscribeSpy<T> onSubscribeSpy;
    private final MultiOnTerminationSpy<T> onTerminationSpy;

    MultiGlobalSpy(Multi<T> upstream) {
        super(upstream);
        onCancellationSpy = Spy.onCancellation(upstream);
        onCompletionSpy = Spy.onCompletion(onCancellationSpy);
        onFailureSpy = Spy.onFailure(onCompletionSpy);
        onItemSpy = Spy.onItem(onFailureSpy);
        onRequestSpy = Spy.onRequest(onItemSpy);
        onSubscribeSpy = Spy.onSubscribe(onRequestSpy);
        onTerminationSpy = Spy.onTermination(onSubscribeSpy);
    }

    public MultiOnCancellationSpy<T> onCancellationSpy() {
        return onCancellationSpy;
    }

    public MultiOnCompletionSpy<T> onCompletionSpy() {
        return onCompletionSpy;
    }

    public MultiOnFailureSpy<T> onFailureSpy() {
        return onFailureSpy;
    }

    public MultiOnItemSpy<T> onItemSpy() {
        return onItemSpy;
    }

    public MultiOnRequestSpy<T> onRequestSpy() {
        return onRequestSpy;
    }

    public MultiOnSubscribeSpy<T> onSubscribeSpy() {
        return onSubscribeSpy;
    }

    public MultiOnTerminationSpy<T> onTerminationSpy() {
        return onTerminationSpy;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        onTerminationSpy.subscribe().withSubscriber(downstream);
    }

    @Override
    public long invocationCount() {
        return onCancellationSpy.invocationCount() + onCompletionSpy.invocationCount() + onFailureSpy.invocationCount()
                + onItemSpy.invocationCount() + onRequestSpy.invocationCount() + onSubscribeSpy.invocationCount()
                + onTerminationSpy.invocationCount();
    }

    @Override
    public boolean invoked() {
        return invocationCount() > 0;
    }

    @Override
    public void reset() {
        onCancellationSpy.reset();
        onCompletionSpy.reset();
        onFailureSpy.reset();
        onItemSpy.reset();
        onRequestSpy.reset();
        onSubscribeSpy.reset();
        onTerminationSpy.reset();
    }

    @Override
    public String toString() {
        return "MultiGlobalSpy{" +
                "onCancellationSpy=" + onCancellationSpy +
                ", onCompletionSpy=" + onCompletionSpy +
                ", onFailureSpy=" + onFailureSpy +
                ", onItemSpy=" + onItemSpy +
                ", onRequestSpy=" + onRequestSpy +
                ", onSubscribeSpy=" + onSubscribeSpy +
                ", onTerminationSpy=" + onTerminationSpy +
                "} " + super.toString();
    }
}
