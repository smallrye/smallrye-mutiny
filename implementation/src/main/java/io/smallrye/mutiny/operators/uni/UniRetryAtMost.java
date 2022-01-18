package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniRetryAtMost<T> extends UniOperator<T, T> {
    private final Predicate<? super Throwable> predicate;
    private final long maxAttempts;

    public UniRetryAtMost(Uni<T> upstream, Predicate<? super Throwable> predicate, long maxAttempts) {
        super(nonNull(upstream, "upstream"));
        this.predicate = nonNull(predicate, "predicate");
        this.maxAttempts = positive(maxAttempts, "maxAttempts");
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniRetryAtMostProcessor<>(this, subscriber));
    }

    private static class UniRetryAtMostProcessor<T> extends UniOperatorProcessor<T, T> {

        private final UniRetryAtMost<T> uniRetryAtMost;

        private volatile int counter = 0;
        private static final AtomicIntegerFieldUpdater<UniRetryAtMostProcessor> counterUpdater = AtomicIntegerFieldUpdater
                .newUpdater(UniRetryAtMostProcessor.class, "counter");

        public UniRetryAtMostProcessor(UniRetryAtMost<T> uniRetryAtMost, UniSubscriber<? super T> downstream) {
            super(downstream);
            this.uniRetryAtMost = uniRetryAtMost;
        }

        @Override
        public void onSubscribe(UniSubscription subscription) {
            int count = counterUpdater.incrementAndGet(this);
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                if (count == 1) {
                    downstream.onSubscribe(this);
                }
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (isCancelled()) {
                Infrastructure.handleDroppedException(failure);
                return;
            }
            if (!testPredicate(failure)) {
                return;
            }
            if (counter > uniRetryAtMost.maxAttempts) {
                downstream.onFailure(failure);
                return;
            }
            UniSubscription previousSubscription = getAndSetUpstreamSubscription(null);
            if (previousSubscription != null) {
                previousSubscription.cancel();
            }
            AbstractUni.subscribe(uniRetryAtMost.upstream(), this);
        }

        private boolean testPredicate(Throwable failure) {
            boolean passes;
            try {
                passes = uniRetryAtMost.predicate.test(failure);
            } catch (Throwable e) {
                downstream.onFailure(new CompositeException(e, failure));
                return false;
            }
            if (!passes) {
                downstream.onFailure(failure);
                return false;
            } else {
                return true;
            }
        }
    }
}
