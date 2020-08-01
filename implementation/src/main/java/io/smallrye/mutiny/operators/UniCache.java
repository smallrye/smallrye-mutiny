package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCache<I> extends UniOperator<I, I> implements UniSubscriber<I> {

    private static final int NOT_INITIALIZED = 0;
    private static final int SUBSCRIBING = 1;
    private static final int SUBSCRIBED = 2;
    private static final int COMPLETED = 3;
    private final AtomicReference<UniSubscription> subscription = new AtomicReference<>();
    private final List<UniSubscriber<? super I>> subscribers = new ArrayList<>();
    private final BiPredicate<I, Throwable> validator;
    private int state = NOT_INITIALIZED;
    private I item;
    private Throwable failure;

    UniCache(Uni<? extends I> upstream, BiPredicate<I, Throwable> validator) {
        super(nonNull(upstream, "upstream"));
        this.validator = nonNull(validator, "validator");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        Runnable action = null;
        synchronized (this) {
            switch (state) {
                case NOT_INITIALIZED:
                    // First subscriber,
                    state = SUBSCRIBING;
                    action = () -> AbstractUni.subscribe(upstream(), this);
                    subscribers.add(subscriber);
                    break;
                case SUBSCRIBING:
                    // Subscription pending
                    subscribers.add(subscriber);
                    break;
                case SUBSCRIBED:
                    // No item yet, but we can provide a Subscription
                    subscribers.add(subscriber);
                    action = () -> subscriber.onSubscribe(() -> onCancellation(subscriber));
                    break;
                case COMPLETED:
                    // Result already computed
                    action = handleCompleted(subscriber);
                    break;
                default:
                    throw new IllegalStateException("Unknown state: " + state);
            }
        }

        // Execute outside of the synchronized await
        if (action != null) {
            action.run();
        }
    }

    private synchronized void onCancellation(UniSubscriber<? super I> subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Handling a new subscriber this UniCache is already {@link #COMPLETED}.
     * Verifies the cached result against the {@link #validator} and
     * either replays the cached result or renews our own subscription
     * to restart the cache lifecycle.
     *
     * @param subscriber the newly subscribed {@link UniSubscriber}
     * @return the {@link Runnable} to be executed afterwards
     */
    private Runnable handleCompleted(UniSubscriber<? super I> subscriber) {
        // Closure of item and failure for validity checking and replaying the same objects
        I item = this.item;
        Throwable failure = this.failure;

        return () -> {
            if (validator.test(item, failure)) {
                // We must first pass a subscription
                subscriber.onSubscribe(() -> onCancellation(subscriber));
                // replay the item for that subscriber
                if (failure != null) {
                    subscriber.onFailure(failure);
                } else {
                    subscriber.onItem(item);
                }
            } else {
                // Items isn't valid any more
                // Our own subscription has to be renewed if not already in process.
                boolean resubscribe = false;
                synchronized (this) {
                    subscribers.add(subscriber);
                    // other subscriptions may be raced into this branch too and triggered re-subscription before
                    if (state == COMPLETED) {
                        state = SUBSCRIBING;
                        subscription.set(null);
                        this.item = null;
                        this.failure = null;
                        resubscribe = true;
                    }
                }
                if (resubscribe) {
                    AbstractUni.subscribe(upstream(), this);
                }
            }
        };
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        List<UniSubscriber<? super I>> list;
        synchronized (this) {
            if (!this.subscription.compareAndSet(null, subscription)) {
                throw new IllegalStateException("Invalid state - received a second subscription from source");
            }
            state = SUBSCRIBED;
            list = new ArrayList<>(subscribers);
        }
        list.forEach(s -> s.onSubscribe(() -> onCancellation(s)));
    }

    @Override
    public void onItem(I item) {
        List<UniSubscriber<? super I>> list;
        synchronized (this) {
            if (state != SUBSCRIBED) {
                throw new IllegalStateException(
                        "Invalid state - received item while we where not in the SUBSCRIBED state, current state is: "
                                + state);
            }
            state = COMPLETED;
            this.item = item;
            list = new ArrayList<>(subscribers);
            // Clear the list
            this.subscribers.clear();
        }
        // Here we may notify a subscriber that would have cancelled its subscription just after the synchronized await
        // we consider it as pending cancellation.
        list.forEach(s -> s.onItem(item));
    }

    @Override
    public void onFailure(Throwable failure) {
        List<UniSubscriber<? super I>> list;
        synchronized (this) {
            if (state != SUBSCRIBED) {
                throw new IllegalStateException(
                        "Invalid state - received item while we where not in the SUBSCRIBED state, current state is: "
                                + state);
            }
            state = COMPLETED;
            this.failure = failure;
            list = new ArrayList<>(subscribers);
            // Clear the list
            this.subscribers.clear();
        }
        // Here we may notify a subscriber that would have cancelled its subscription just after the synchronized await
        // we consider it as pending cancellation.
        list.forEach(s -> s.onFailure(failure));
    }
}
