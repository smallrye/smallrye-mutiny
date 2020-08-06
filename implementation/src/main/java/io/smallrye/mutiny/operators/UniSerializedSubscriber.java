package io.smallrye.mutiny.operators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * An implementation of {@link UniSubscriber} and {@link UniSubscription} making sure event handlers are only called once.
 */
public class UniSerializedSubscriber<T> implements UniSubscriber<T>, UniSubscription {

    private static final int INIT = 0;
    /**
     * Got a downstream subscriber.
     */
    private static final int SUBSCRIBED = 1;

    /**
     * Got a subscription from upstream.
     */
    private static final int HAS_SUBSCRIPTION = 2;

    /**
     * Got a failure or item from upstream
     */
    private static final int DONE = 3;

    private final AtomicInteger state = new AtomicInteger(INIT);
    private final AbstractUni<T> upstream;
    private final UniSubscriber<? super T> downstream;

    private UniSubscription subscription;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    UniSerializedSubscriber(AbstractUni<T> upstream, UniSubscriber<? super T> subscriber) {
        this.upstream = ParameterValidation.nonNull(upstream, "source");
        this.downstream = ParameterValidation.nonNull(subscriber, "subscriber` must not be `null`");
    }

    // TODO Caught RuntimeException thrown by the onItem and onFailure and log them accordingly

    public static <T> void subscribe(AbstractUni<T> source, UniSubscriber<? super T> subscriber) {
        UniSubscriber<? super T> actual = Infrastructure.onUniSubscription(source, subscriber);
        UniSerializedSubscriber<T> wrapped = new UniSerializedSubscriber<>(source, actual);
        wrapped.subscribe();
    }

    private void subscribe() {
        if (state.compareAndSet(INIT, SUBSCRIBED)) {
            upstream.subscribing(this);
        }
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        ParameterValidation.nonNull(subscription, "subscription");

        if (state.compareAndSet(SUBSCRIBED, HAS_SUBSCRIPTION)) {
            this.subscription = subscription;
            this.downstream.onSubscribe(this);
        } else if (state.get() == DONE) {
            Throwable collected = failure.getAndSet(null);
            if (collected != null) {
                this.downstream.onSubscribe(this);
                this.downstream.onFailure(collected);
            }
        } else {
            EmptyUniSubscription.propagateFailureEvent(this.downstream,
                    new IllegalStateException(
                            "Invalid transition, expected to be in the SUBSCRIBED state but was in " + state));
        }
    }

    @Override
    public void onItem(T item) {
        if (state.compareAndSet(SUBSCRIBED, DONE)) {
            EmptyUniSubscription.propagateFailureEvent(this.downstream,
                    new IllegalStateException(
                            "Invalid transition, expected to be in the HAS_SUBSCRIPTION states but was in SUBSCRIBED"));
        } else if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            downstream.onItem(item);
        }
        dispose();
    }

    @Override
    public void onFailure(Throwable failure) {
        if (state.compareAndSet(SUBSCRIBED, DONE)) {
            EmptyUniSubscription.propagateFailureEvent(this.downstream,
                    new IllegalStateException(
                            "Invalid transition, expected to be in the HAS_SUBSCRIPTION states but was in "
                                    + state));
        } else if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            downstream.onFailure(failure);
            dispose();
        }
    }

    private void dispose() {
        subscription = null;
    }

    @Override
    public void cancel() {
        if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            subscription.cancel();
            dispose();
        } else if (state.compareAndSet(SUBSCRIBED, DONE)) {
            dispose();
        }
    }

    public synchronized boolean isCancelledOrDone() {
        return state.get() == DONE;
    }
}
