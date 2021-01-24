package io.smallrye.mutiny.operators.uni;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.tuples.Functions;

public class UniOnTermination<T> extends UniOperator<T, T> {
    private final Functions.TriConsumer<T, Throwable, Boolean> callback;

    public UniOnTermination(Uni<T> upstream, Functions.TriConsumer<T, Throwable, Boolean> callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnTerminationProcessor(subscriber));
    }

    private class UniOnTerminationProcessor extends UniOperatorProcessor<T, T> {

        public UniOnTerminationProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(T item) {
            if (!isCancelled()) {
                try {
                    callback.accept(item, null, false);
                } catch (Throwable e) {
                    downstream.onFailure(e);
                    return;
                }
                downstream.onItem(item);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isCancelled()) {
                try {
                    callback.accept(null, failure, false);
                } catch (Throwable e) {
                    downstream.onFailure(new CompositeException(failure, e));
                    return;
                }
                downstream.onFailure(failure);
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }

        @Override
        public void cancel() {
            if (!isCancelled()) {
                super.cancel();
                callback.accept(null, null, true);
            }
        }
    }
}
