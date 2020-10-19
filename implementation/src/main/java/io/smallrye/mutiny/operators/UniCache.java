package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCache<I> extends UniOperator<I, I> implements UniSubscriber<I> {

    private enum State {
        INIT,
        SUBSCRIBING,
        SUBSCRIBED,
        CACHING
    }

    private final BooleanSupplier invalidationRequested;

    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

    private final AtomicInteger wip = new AtomicInteger();
    private final List<UniSerializedSubscriber<? super I>> awaitingSubscription = synchronizedList(new ArrayList<>());
    private final List<UniSerializedSubscriber<? super I>> awaitingResult = synchronizedList(new ArrayList<>());

    private volatile UniSubscription upstreamSubscription;
    private volatile I item;
    private volatile Throwable failure;

    public UniCache(Uni<? extends I> upstream) {
        this(upstream, () -> false);
    }

    public UniCache(Uni<? extends I> upstream, BooleanSupplier invalidationRequested) {
        super(nonNull(upstream, "upstream"));
        this.invalidationRequested = invalidationRequested;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        awaitingSubscription.add(subscriber);
        if (invalidationRequested.getAsBoolean() && state.get() != State.SUBSCRIBING) {
            state.set(State.INIT);
            if (upstreamSubscription != null) {
                upstreamSubscription.cancel();
            }
        }
        if (state.compareAndSet(State.INIT, State.SUBSCRIBING)) {
            // This thread is performing the upstream subscription
            AbstractUni.subscribe(upstream(), this);
        }
        drain();
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        upstreamSubscription = subscription;
        if (!state.compareAndSet(State.SUBSCRIBING, State.SUBSCRIBED)) {
            throw new IllegalStateException("Current state is " + state.get() + " but should be " + State.SUBSCRIBING);
        }
        drain();
    }

    private void drain() {
        // Check if another thread is working
        if (wip.getAndIncrement() != 0) {
            return;
        }

        // Big loop
        int missed = 1;
        for (;;) {

            ArrayList<UniSerializedSubscriber<? super I>> subscribers;
            I currentItem;
            Throwable currentFailure;

            if (!awaitingSubscription.isEmpty()) {
                // Make a safe copy of the subscribers awaiting for a subscription
                synchronized (awaitingSubscription) {
                    subscribers = new ArrayList<>(awaitingSubscription);
                }

                // Handle the subscribers that are awaiting a subscription
                for (UniSerializedSubscriber<? super I> subscriber : subscribers) {
                    currentItem = item;
                    currentFailure = failure;
                    State state = this.state.get();
                    switch (state) {
                        case INIT:
                        case SUBSCRIBING:
                            break;
                        case SUBSCRIBED:
                            subscriber.onSubscribe(() -> tryCancelSubscription(subscriber));
                            awaitingSubscription.remove(subscriber);
                            awaitingResult.add(subscriber);
                            break;
                        case CACHING:
                            subscriber.onSubscribe(() -> tryCancelSubscription(subscriber));
                            if (currentFailure != null) {
                                subscriber.onFailure(currentFailure);
                            } else {
                                subscriber.onItem(currentItem);
                            }
                            awaitingSubscription.remove(subscriber);
                            awaitingResult.remove(subscriber);
                            break;
                        default:
                            throw new IllegalStateException("Current state is " + state);
                    }
                }
            }

            if (!awaitingResult.isEmpty()) {
                // Make a safe copy of the subscribers awaiting a result
                synchronized (awaitingResult) {
                    subscribers = new ArrayList<>(awaitingResult);
                }
                // Handle the subscribers that are awaiting a result
                for (UniSerializedSubscriber<? super I> subscriber : subscribers) {
                    currentItem = item;
                    currentFailure = failure;
                    if (state.get() == State.CACHING) {
                        if (failure != null) {
                            subscriber.onFailure(currentFailure);
                        } else {
                            subscriber.onItem(currentItem);
                        }
                        awaitingResult.remove(subscriber);
                    }
                }
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    private void tryCancelSubscription(UniSerializedSubscriber<? super I> subscriber) {
        awaitingSubscription.remove(subscriber);
        awaitingResult.remove(subscriber);
    }

    @Override
    public void onItem(I item) {
        this.item = item;
        this.failure = null;
        checkStateCorrectness();
        drain();
    }

    @Override
    public void onFailure(Throwable failure) {
        this.item = null;
        this.failure = failure;
        checkStateCorrectness();
        drain();
    }

    private void checkStateCorrectness() {
        switch (state.get()) {
            case INIT:
            case SUBSCRIBING:
                return;
            default:
                // TODO maybe there's an issue with concurrent invalidations
                if (!state.compareAndSet(State.SUBSCRIBED, State.CACHING)) {
                    throw new IllegalStateException("Current state is " + state + " but should be " + State.SUBSCRIBED);
                }
        }
    }
}
