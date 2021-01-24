package io.smallrye.mutiny.operators.uni;

import java.util.function.Consumer;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniOnItemConsume<T> extends UniOperator<T, T> {

    private final Consumer<? super T> onItemCallback;
    private final Consumer<Throwable> onFailureCallback;
    private final Predicate<? super Throwable> onFailurePredicate;

    public UniOnItemConsume(Uni<? extends T> upstream,
            Consumer<? super T> onItemCallback,
            Consumer<Throwable> onFailureCallback, Predicate<? super Throwable> predicate) {
        super(upstream);
        this.onItemCallback = onItemCallback;
        this.onFailureCallback = onFailureCallback;
        this.onFailurePredicate = predicate;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnItemComsumeProcessor(subscriber));
    }

    private class UniOnItemComsumeProcessor extends UniOperatorProcessor<T, T> {

        public UniOnItemComsumeProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(T item) {
            if (!isCancelled()) {
                if (invokeEventHandler(onItemCallback, item, false, downstream)) {
                    downstream.onItem(item);
                }
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isCancelled()) {
                if (onFailurePredicate != null) {
                    try {
                        if (onFailurePredicate.test(failure)) {
                            if (invokeEventHandler(onFailureCallback, failure, true, downstream)) {
                                downstream.onFailure(failure);
                            }
                        } else {
                            downstream.onFailure(failure);
                        }
                    } catch (Throwable e) {
                        downstream.onFailure(new CompositeException(failure, e));
                    }
                } else {
                    if (invokeEventHandler(onFailureCallback, failure, true, downstream)) {
                        downstream.onFailure(failure);
                    }
                }
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }

        private <E> boolean invokeEventHandler(Consumer<? super E> handler, E event, boolean wasCalledByOnFailure,
                UniSubscriber<? super T> subscriber) {
            if (handler != null) {
                try {
                    handler.accept(event);
                } catch (Throwable e) {
                    if (wasCalledByOnFailure) {
                        subscriber.onFailure(new CompositeException((Throwable) event, e));
                    } else {
                        subscriber.onFailure(e);
                    }
                    return false;
                }
            }
            return true;
        }
    }
}
