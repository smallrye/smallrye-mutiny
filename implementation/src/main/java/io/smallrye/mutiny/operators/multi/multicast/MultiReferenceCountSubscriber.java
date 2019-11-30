package io.smallrye.mutiny.operators.multi.multicast;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;

public class MultiReferenceCountSubscriber<T> extends MultiOperatorProcessor<T, T> {

    private final AtomicBoolean done = new AtomicBoolean();
    private final MultiReferenceCount<T> parent;
    private final ConnectableMultiConnection connection;

    MultiReferenceCountSubscriber(Subscriber<? super T> downstream, MultiReferenceCount<T> parent,
            ConnectableMultiConnection connection) {
        super(downstream);
        this.parent = parent;
        this.connection = connection;
    }

    @Override
    public void onNext(T t) {
        downstream.onNext(t);
    }

    @Override
    public void onError(Throwable failure) {
        if (done.compareAndSet(false, true)) {
            parent.terminated(connection);
            super.onError(failure);
        }
    }

    @Override
    public void onComplete() {
        if (done.compareAndSet(false, true)) {
            parent.terminated(connection);
            super.onComplete();
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
