package io.smallrye.mutiny.operators.multi.multicast;

import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiReferenceCountSubscriber<T> extends MultiOperatorProcessor<T, T> {

    private final AtomicBoolean done = new AtomicBoolean();
    private final MultiReferenceCount<T> parent;
    private final ConnectableMultiConnection connection;

    MultiReferenceCountSubscriber(MultiSubscriber<? super T> downstream, MultiReferenceCount<T> parent,
            ConnectableMultiConnection connection) {
        super(downstream);
        this.parent = parent;
        this.connection = connection;
    }

    @Override
    public void onItem(T t) {
        downstream.onItem(t);
    }

    @Override
    public void onFailure(Throwable failure) {
        if (done.compareAndSet(false, true)) {
            parent.terminated(connection);
            super.onFailure(failure);
        } else {
            Infrastructure.handleDroppedException(failure);
        }
    }

    @Override
    public void onCompletion() {
        if (done.compareAndSet(false, true)) {
            parent.terminated(connection);
            super.onCompletion();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        if (done.compareAndSet(false, true)) {
            parent.cancel(connection);
        }
    }
}
