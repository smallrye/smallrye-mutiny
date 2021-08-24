package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;

import java.util.concurrent.atomic.AtomicReference;

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
        AbstractUni.subscribe(upstream(), new UniOnCancellationProcessor(subscriber));
    }

    private enum State {
        INIT,
        DONE,
        CANCELLED
    }

    private class UniOnCancellationProcessor extends UniOperatorProcessor<T, T> {

        public UniOnCancellationProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

        @Override
        public void onItem(T item) {
            if (state.compareAndSet(State.INIT, State.DONE)) {
                downstream.onItem(item);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (state.compareAndSet(State.INIT, State.DONE)) {
                downstream.onFailure(failure);
            }
        }

        @Override
        public void cancel() {
            if (state.compareAndSet(State.INIT, State.CANCELLED)) {
                UniSubscription sub = getAndSetUpstreamSubscription(CANCELLED);
                callback.run();
                if (sub != null) {
                    sub.cancel();
                }
            }
        }
    }
}
