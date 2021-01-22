package io.smallrye.mutiny.operators.uni;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniDelegatingSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnItemOrFailureMap<I, O> extends UniOperator<I, O> {

    private final BiFunction<? super I, Throwable, ? extends O> mapper;

    public UniOnItemOrFailureMap(Uni<I> upstream, BiFunction<? super I, Throwable, ? extends O> mapper) {
        super(upstream);
        this.mapper = mapper;
    }

    @Override
    protected void subscribing(UniSubscriber<? super O> downstream) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, O>(downstream) {

            private final AtomicBoolean done = new AtomicBoolean();

            @Override
            public void onSubscribe(UniSubscription subscription) {
                super.onSubscribe(() -> {
                    done.set(true);
                    subscription.cancel();
                });
            }

            @Override
            public void onItem(I item) {
                if (done.compareAndSet(false, true)) {
                    O outcome;
                    try {
                        outcome = mapper.apply(item, null);
                        // We cannot call onItem here, as if onItem would throw an exception
                        // it would be caught and onFailure would be called. This would be illegal.
                    } catch (Throwable e) { // NOSONAR
                        // Be sure to not call the mapper again with the failure.
                        downstream.onFailure(e);
                        return;
                    }

                    downstream.onItem(outcome);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (done.compareAndSet(false, true)) {
                    O outcome;
                    try {
                        outcome = mapper.apply(null, failure);
                        // We cannot call onItem here, as if onItem would throw an exception
                        // it would be caught and onFailure would be called. This would be illegal.
                    } catch (Throwable e) { // NOSONAR
                        // Be sure to not call the mapper again with the failure.
                        downstream.onFailure(new CompositeException(failure, e));
                        return;
                    }

                    downstream.onItem(outcome);
                }
            }
        });
    }
}
