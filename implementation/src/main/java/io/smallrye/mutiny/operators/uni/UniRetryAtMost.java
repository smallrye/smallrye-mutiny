package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import io.smallrye.mutiny.Uni;
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
        AtomicInteger numberOfSubscriptions = new AtomicInteger(0);
        UniSubscriber<T> retryingSubscriber = new UniSubscriber<T>() {
            final AtomicReference<UniSubscription> reference = new AtomicReference<>();

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
                    if (!test(subscriber, failure)) {
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

        AbstractUni.subscribe(upstream(), retryingSubscriber);
    }

    private void resubscribe(Uni<? extends T> upstream, UniSubscriber<T> subscriber) {
        AbstractUni.subscribe(upstream, subscriber);
    }

    private boolean test(
            UniSubscriber<? super T> subscriber, Throwable failure) {
        boolean pass;
        try {
            pass = predicate.test(failure);
        } catch (Throwable e) {
            subscriber.onFailure(e);
            return false;
        }
        if (!pass) {
            subscriber.onFailure(failure);
            return false;
        } else {
            return true;
        }
    }
}
