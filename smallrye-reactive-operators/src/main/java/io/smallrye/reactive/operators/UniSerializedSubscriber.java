package io.smallrye.reactive.operators;

import io.smallrye.reactive.infrastructure.Infrastructure;
import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.propagateFailureEvent;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

/**
 * An implementation of {@link UniSubscriber} and {@link UniSubscription} making sure event handlers are only called once.
 */
public class UniSerializedSubscriber<T> implements UniSubscriber<T>, UniSubscription {

    private static final int INIT = 0;
    private static final int SUBSCRIBED = 1;
    private static final int HAS_SUBSCRIPTION = 2;
    private static final int DONE = 3; // Terminal state

    private final AtomicInteger state = new AtomicInteger(INIT);
    private final AbstractUni<T> source;
    private final UniSubscriber<? super T> downstream;
    private UniSubscription upstream;
    private AtomicReference<Throwable> collectedFailure = new AtomicReference<>();

    private UniSerializedSubscriber(AbstractUni<T> source, UniSubscriber<? super T> subscriber) {
        this.source = nonNull(source, "source");
        this.downstream = nonNull(subscriber, "subscriber` must not be `null`");
    }

    // TODO Caught RuntimeException thrown by the onItem and onFailure and log them accordingly

    public static <T> void subscribe(AbstractUni<T> source, UniSubscriber<? super T> subscriber) {
        UniSubscriber<? super T> actual  = Infrastructure.onUniSubscription(source, subscriber);
        UniSerializedSubscriber<T> wrapped = new UniSerializedSubscriber<>(source, actual);
        wrapped.subscribe();
    }

    private void subscribe() {
        if (state.compareAndSet(INIT, SUBSCRIBED)) {
            this.source.subscribing(this);
        } else {
            propagateFailureEvent(this.downstream,
                    new IllegalStateException("Unable to subscribe, already got a subscriber"));
        }
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        nonNull(subscription, "subscription");

        if (state.compareAndSet(SUBSCRIBED, HAS_SUBSCRIPTION)) {
            this.upstream = subscription;
            this.downstream.onSubscribe(this);
        } else if (state.get() == DONE) {
            Throwable collected = collectedFailure.getAndSet(null);
            if (collected != null) {
                this.downstream.onFailure(collected);
            }
        } else {
            propagateFailureEvent(this.downstream,
                    new IllegalStateException(
                            "Invalid transition, expected to be in the SUBSCRIBED state but was in " + state.get()));
        }
    }

    @Override
    public void onItem(T item) {
        if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            downstream.onItem(item);
            dispose();
        } else if (state.get() != DONE) { // Are we already done? In this case, drop the signal
            propagateFailureEvent(this.downstream,
                    new IllegalStateException(
                            "Invalid transition, expected to be in the HAS_SUBSCRIPTION state but was in " + state
                                    .get()));
        }
    }

    @Override
    public void onFailure(Throwable failure) {
        if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            downstream.onFailure(failure);
        } else if (state.compareAndSet(SUBSCRIBED, DONE)) {
            collectedFailure.compareAndSet(null, failure);
        } else if (state.get() != DONE) { // Are we already done? In this case, drop the signal
            propagateFailureEvent(this.downstream,
                    new IllegalStateException(
                            "Invalid transition, expected to be in the HAS_SUBSCRIPTION state but was in " + state
                                    .get()));
        }
    }

    private void dispose() {
        upstream = null;
    }

    @Override
    public void cancel() {
        if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            upstream.cancel();
            dispose();
        }
    }

    public boolean isCancelledOrDone() {
        return state.get() == DONE;
    }
}
