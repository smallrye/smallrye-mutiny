package io.smallrye.mutiny.operators.uni;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

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

    private class UniOnCancellationProcessor extends UniOperatorProcessor<T, T> {

        public UniOnCancellationProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void cancel() {
            callback.run();
            super.cancel();
        }
    }
}
