package io.smallrye.mutiny.operators.uni;

import java.util.function.BiConsumer;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniOnItemOrFailureConsume<T> extends UniOperator<T, T> {

    private final BiConsumer<? super T, Throwable> callback;

    public UniOnItemOrFailureConsume(Uni<? extends T> upstream,
            BiConsumer<? super T, Throwable> callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnItemOrFailureConsumeProcessor(subscriber));
    }

    private class UniOnItemOrFailureConsumeProcessor extends UniOperatorProcessor<T, T> {

        public UniOnItemOrFailureConsumeProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(T item) {
            if (!isCancelled()) {
                if (invokeCallback(item, null, downstream)) {
                    downstream.onItem(item);
                }
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isCancelled()) {
                if (invokeCallback(null, failure, downstream)) {
                    downstream.onFailure(failure);
                }
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }

        private boolean invokeCallback(T item, Throwable failure, UniSubscriber<? super T> subscriber) {
            try {
                callback.accept(item, failure);
                return true;
            } catch (Throwable e) {
                if (failure != null) {
                    subscriber.onFailure(new CompositeException(failure, e));
                } else {
                    subscriber.onFailure(e);
                }
                return false;
            }
        }
    }
}
