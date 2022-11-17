package io.smallrye.mutiny.subscription;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;

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

    private volatile UniSubscription subscription;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    public UniSerializedSubscriber(AbstractUni<T> upstream, UniSubscriber<? super T> subscriber) {
        this.upstream = ParameterValidation.nonNull(upstream, "source");
        this.downstream = ParameterValidation.nonNull(subscriber, "subscriber` must not be `null`");
    }

    public static <T> void subscribe(AbstractUni<T> source, UniSubscriber<? super T> subscriber) {
        UniSubscriber<? super T> actual = Infrastructure.onUniSubscription(source, subscriber);
        UniSerializedSubscriber<T> wrapped = new UniSerializedSubscriber<>(source, actual);
        wrapped.subscribe();
    }

    private void subscribe() {
        if (state.compareAndSet(INIT, SUBSCRIBED)) {
            upstream.subscribe(this);
        }
    }

    @Override
    public Context context() {
        return downstream.context();
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
        if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            try {
                downstream.onItem(item);
            } catch (Throwable e) {
                Infrastructure.handleDroppedException(e);
                throw e; // Rethrow in case of synchronous emission
            }
        } else if (state.compareAndSet(SUBSCRIBED, DONE)) {
            failure.set(new IllegalStateException(
                    "Invalid transition, expected to be in the HAS_SUBSCRIPTION states but was in SUBSCRIBED and received onItem("
                            + item + ")"));
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            try {
                downstream.onFailure(throwable);
            } catch (Throwable e) {
                Infrastructure.handleDroppedException(new CompositeException(throwable, e));
                throw e; // Rethrow in case of synchronous emission
            }
        } else if (state.compareAndSet(SUBSCRIBED, DONE)) {
            failure.set(throwable);
        } else {
            Infrastructure.handleDroppedException(throwable);
        }
    }

    @Override
    public void cancel() {
        if (state.compareAndSet(HAS_SUBSCRIPTION, DONE)) {
            while (subscription == null) {
                // We are in the middle of a race condition with onSubscription()
                Thread.onSpinWait();
            }
            if (subscription != null) { // May have been cancelled already by another thread.
                subscription.cancel();
            }
        } else {
            state.set(DONE);
        }
    }
}
