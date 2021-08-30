package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnCancellation<T> extends UniOperator<T, T> {

    private final Runnable callback;

    public UniOnCancellation(Uni<T> upstream, Runnable callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnCancellationProcessor<T>(callback, subscriber));
    }

    private enum State {
        INIT,
        DONE,
        CANCELLED
    }

    private static class UniOnCancellationProcessor<T> extends UniOperatorProcessor<T, T> {

        private final Runnable callback;

        private volatile State state = State.INIT;
        private static final AtomicReferenceFieldUpdater<UniOnCancellationProcessor, State> stateUpdater = AtomicReferenceFieldUpdater
                .newUpdater(UniOnCancellationProcessor.class, State.class, "state");

        public UniOnCancellationProcessor(Runnable callback, UniSubscriber<? super T> downstream) {
            super(downstream);
            this.callback = callback;
        }

        @Override
        public void onItem(T item) {
            if (stateUpdater.compareAndSet(this, State.INIT, State.DONE)) {
                downstream.onItem(item);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (stateUpdater.compareAndSet(this, State.INIT, State.DONE)) {
                downstream.onFailure(failure);
            }
        }

        @Override
        public void cancel() {
            if (stateUpdater.compareAndSet(this, State.INIT, State.CANCELLED)) {
                UniSubscription sub = getAndSetUpstreamSubscription(CANCELLED);
                callback.run();
                if (sub != null) {
                    sub.cancel();
                }
            }
        }
    }
}
