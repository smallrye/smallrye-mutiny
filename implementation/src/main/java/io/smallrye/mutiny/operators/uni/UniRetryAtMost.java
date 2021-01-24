package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

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
        AbstractUni.subscribe(upstream(), new UniRetryAtMostProcessor(subscriber));
    }

    private class UniRetryAtMostProcessor extends UniOperatorProcessor<T, T> {

        private final AtomicInteger counter = new AtomicInteger(0);

        public UniRetryAtMostProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(UniSubscription subscription) {
            counter.incrementAndGet();
            super.onSubscribe(subscription);
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
            if (counter.get() > maxAttempts) {
                downstream.onFailure(failure);
                return;
            }
            UniSubscription previousSubscription = upstream.getAndSet(null);
            if (previousSubscription != null) {
                previousSubscription.cancel();
            }
            AbstractUni.subscribe(upstream(), this);
        }

        private boolean testPredicate(Throwable failure) {
            boolean passes;
            try {
                passes = predicate.test(failure);
            } catch (Throwable e) {
                downstream.onFailure(e);
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
