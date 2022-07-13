package io.smallrye.mutiny.streams.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.StrictMultiSubscriber;
import io.smallrye.mutiny.helpers.Subscriptions;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * A processor forwarding to a subscriber. This is used to connect a "next to be" producer.
 */
public class ConnectableProcessor<T> implements Processor<T, T> {

    /**
     * Reference of the subscriber if any.
     * If set the state is HAS_SUBSCRIBER+
     */
    private final AtomicReference<Subscriber<? super T>> subscriber = new AtomicReference<>();
    /**
     * Reference on the subscription if any.
     * If set the state is HAS_SUBSCRIPTION+
     */

    private final AtomicReference<Subscription> subscription = new AtomicReference<>();
    /**
     * Reported failure if any.
     * If set the state if FAILED
     */
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    /**
     * Current state.
     */
    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);

        // Set the subscriber, if we already have one report an error as we do not support multicasting.
        if (!this.subscriber.compareAndSet(null,
                AdaptersToReactiveStreams.subscriber(new StrictMultiSubscriber<>(AdaptersToFlow.subscriber(subscriber))))) {
            Subscriptions.fail(AdaptersToFlow.subscriber(subscriber), new IllegalStateException("Multicasting not supported"));
            return;
        }

        // Attempt to fix https://github.com/smallrye/smallrye-reactive-streams-operators/issues/63
        // We need to be sure that the transition is only executed by a single thread.
        // So the other threads are blocked and wait until the final state is computed.
        synchronized (this) {
            // Set the state, if failed, report the error
            if (!state.compareAndSet(State.IDLE, State.HAS_SUBSCRIBER)) {
                // We were not in the idle state, the behavior depends on our current state
                // For failure and completed, we just creates an empty subscription and immediately
                // report the error or completion
                if (state.get() == State.FAILED) {
                    manageSubscribeInFailedState(subscriber);
                } else if (state.get() == State.COMPLETE) {
                    manageSubscribeInCompleteState(subscriber);
                } else if (state.get() == State.HAS_SUBSCRIPTION) {
                    manageSubscribeInTheHasSubscriptionState(subscriber);
                } else {
                    throw new IllegalStateException("Illegal transition - subscribe happened in the "
                            + state.get().name() + " state");
                }
            }
        }
    }

    private void manageSubscribeInTheHasSubscriptionState(Subscriber<? super T> subscriber) {
        // We already have a subscription, use it.
        // However, we could complete of failed in the meantime.
        subscriber.onSubscribe(
                new WrappedSubscription(subscription.get(),
                        () -> this.subscriber
                                .set(AdaptersToReactiveStreams.subscriber(new Subscriptions.CancelledSubscriber<>()))));
        if (!state.compareAndSet(State.HAS_SUBSCRIPTION, State.PROCESSING)) {
            if (state.get() == State.FAILED) {
                subscriber.onError(failure.get());
            } else if (state.get() == State.COMPLETE) {
                subscriber.onComplete();
            } else {
                throw new IllegalStateException("Illegal transition - subscribe called in the "
                        + state.get().name() + " state");
            }
        }
    }

    private void manageSubscribeInCompleteState(Subscriber<? super T> subscriber) {
        Subscriptions.complete(AdaptersToFlow.subscriber(subscriber));
    }

    private void manageSubscribeInFailedState(Subscriber<? super T> subscriber) {
        Subscriptions.fail(AdaptersToFlow.subscriber(subscriber), failure.get());
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        Objects.requireNonNull(subscription);
        // We already have a subscription, cancel the received one.
        if (!this.subscription.compareAndSet(null, subscription)) {
            subscription.cancel();
            return;
        }

        // Handle the transition: IDLE -> HAS_SUBSCRIPTION.
        if (!state.compareAndSet(State.IDLE, State.HAS_SUBSCRIPTION)) {
            state.set(State.PROCESSING);
            subscriber.get().onSubscribe(new WrappedSubscription(subscription,
                    () -> subscriber.set(AdaptersToReactiveStreams.subscriber(new Subscriptions.CancelledSubscriber<>()))));
        }
    }

    @Override
    public void onNext(T item) {
        Objects.requireNonNull(item);
        Subscriber<? super T> actualSubscriber = this.subscriber.get();
        if (actualSubscriber == null) {
            throw new IllegalStateException("No subscriber - cannot handle onNext");
        } else {
            actualSubscriber.onNext(item);
        }
    }

    @Override
    public synchronized void onComplete() {
        if (state.get() == State.PROCESSING) {
            subscriber.get().onComplete();
            state.set(State.COMPLETE);
        } else if (state.get() == State.FAILED || state.get() == State.COMPLETE || state.get() == State.IDLE) {
            throw new IllegalStateException("Invalid transition, cannot handle onComplete in " + state.get().name());
        } else {
            state.set(State.COMPLETE);
        }
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        Objects.requireNonNull(throwable);
        this.failure.set(throwable);
        if (state.get() == State.PROCESSING) {
            subscriber.get().onError(throwable);
            state.set(State.FAILED);
        } else if (state.get() == State.FAILED || state.get() == State.COMPLETE || state.get() == State.IDLE) {
            throw new IllegalStateException("Invalid transition, cannot handle onError in " + state.get().name());
        } else {
            state.set(State.FAILED);
        }
    }

    private enum State {
        IDLE, // Start state
        HAS_SUBSCRIBER, // When we get a subscriber
        HAS_SUBSCRIPTION, // When we get a subscription
        PROCESSING, // Processing started
        FAILED, // Caught an error, final state
        COMPLETE // Completed, final state
    }

}
