package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.positive;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.helpers.Predicates;
import io.smallrye.reactive.unimulti.subscription.UniSubscriber;
import io.smallrye.reactive.unimulti.subscription.UniSubscription;

public class UniRetryAtMost<T> extends UniOperator<T, T> {
    private final Predicate<? super Throwable> predicate;
    private final long maxAttempts;

    public UniRetryAtMost(Uni<T> upstream, Predicate<? super Throwable> predicate, long maxAttempts) {
        super(nonNull(upstream, "upstream"));
        this.predicate = nonNull(predicate, "predicate");
        this.maxAttempts = positive(maxAttempts, "maxAttempts");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AtomicInteger numberOfSubscriptions = new AtomicInteger(0);
        UniSubscriber<T> retryingSubscriber = new UniSubscriber<T>() {
            AtomicReference<UniSubscription> reference = new AtomicReference<>();

            @Override
            public void onSubscribe(UniSubscription subscription) {
                if (numberOfSubscriptions.getAndIncrement() == 0) {
                    subscriber.onSubscribe(() -> {
                        UniSubscription old = reference.getAndSet(CANCELLED);
                        if (old != null) {
                            old.cancel();
                        }
                    });
                } else {
                    reference.compareAndSet(null, subscription);
                }
            }

            @Override
            public void onItem(T item) {
                if (reference.get() != CANCELLED) {
                    subscriber.onItem(item);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (reference.get() != CANCELLED) {
                    if (!Predicates.testFailure(predicate, subscriber, failure)) {
                        return;
                    }

                    if (numberOfSubscriptions.get() > maxAttempts) {
                        subscriber.onFailure(failure);
                        return;
                    }

                    // retry.
                    UniSubscription old = reference.getAndSet(null);
                    if (old != null) {
                        old.cancel();
                    }
                    resubscribe(upstream(), this);
                }
            }
        };

        upstream().subscribe().withSubscriber(retryingSubscriber);
    }

    private void resubscribe(Uni<? extends T> upstream, UniSubscriber<T> subscriber) {
        upstream.subscribe().withSubscriber(subscriber);
    }
}
