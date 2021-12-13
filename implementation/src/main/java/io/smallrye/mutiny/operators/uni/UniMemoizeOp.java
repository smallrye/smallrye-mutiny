package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniMemoizeOp<I> extends UniOperator<I, I> implements UniSubscriber<I> {

    private enum State {
        INIT,
        SUBSCRIBING,
        SUBSCRIBED,
        CACHING
    }

    private final BooleanSupplier invalidationRequested;

    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

    private final AtomicInteger wip = new AtomicInteger();
    private final List<UniSubscriberWrapper<? super I>> subscribers = synchronizedList(new ArrayList<>());

    private volatile UniSubscription upstreamSubscription;
    private volatile I item;
    private volatile Throwable failure;
    private volatile Context lastContextInUse;

    public UniMemoizeOp(Uni<? extends I> upstream) {
        this(upstream, () -> false);
    }

    public UniMemoizeOp(Uni<? extends I> upstream, BooleanSupplier invalidationRequested) {
        super(nonNull(upstream, "upstream"));
        this.invalidationRequested = invalidationRequested;
    }

    @Override
    public Context context() {
        return lastContextInUse;
    }

    @Override
    public void subscribe(UniSubscriber<? super I> subscriber) {
        if (invalidationRequested.getAsBoolean() && state.get() != State.SUBSCRIBING) {
            state.set(State.INIT);
            if (upstreamSubscription != null) {
                upstreamSubscription.cancel();
            }
        }

        // Wrap the subscriber
        UniSubscriberWrapper<? super I> wrapper = new UniSubscriberWrapper<>(subscriber);

        // Early exit with cached data
        if (state.get() == State.CACHING) {
            subscriber.onSubscribe(wrapper::markCancelled);
            if (!wrapper.isCancelled()) {
                if (failure != null) {
                    subscriber.onFailure(failure);
                } else {
                    subscriber.onItem(item);
                }
            }
            return;
        }

        subscribers.add(wrapper);

        if (state.compareAndSet(State.INIT, State.SUBSCRIBING)) {
            // This thread is performing the upstream subscription
            this.lastContextInUse = subscriber.context();
            AbstractUni.subscribe(upstream(), this);
        }
        drain();
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        if (state.compareAndSet(State.SUBSCRIBING, State.SUBSCRIBED)) {
            upstreamSubscription = subscription;
            drain();
        } else {
            subscription.cancel();
        }
    }

    private void drain() {
        // Check if another thread is working
        if (wip.getAndIncrement() != 0) {
            return;
        }

        // Big loop
        int missed = 1;
        for (;;) {
            ArrayList<UniSubscriberWrapper<? super I>> wrappers;
            I currentItem;
            Throwable currentFailure;

            if (!subscribers.isEmpty()) {
                // Thread-safe copy
                synchronized (subscribers) {
                    wrappers = new ArrayList<>(subscribers);
                }

                // Handle subscribers
                for (UniSubscriberWrapper<? super I> wrapper : wrappers) {
                    if (wrapper.isCancelled()) {
                        subscribers.remove(wrapper);
                        continue;
                    }

                    currentItem = item;
                    currentFailure = failure;
                    State state = this.state.get();

                    if (wrapper.isAwaitingSubscription()) {
                        switch (state) {
                            case INIT:
                            case SUBSCRIBING:
                                break;
                            case SUBSCRIBED:
                                wrapper.subscriber.onSubscribe(wrapper::markCancelled);
                                wrapper.markSubscribed();
                                break;
                            case CACHING:
                                wrapper.subscriber.onSubscribe(wrapper::markCancelled);
                                wrapper.markSubscribed();
                                try {
                                    if (!wrapper.isCancelled()) {
                                        if (currentFailure != null) {
                                            wrapper.subscriber.onFailure(currentFailure);
                                        } else {
                                            wrapper.subscriber.onItem(currentItem);
                                        }
                                    }
                                } finally {
                                    subscribers.remove(wrapper);
                                }
                                break;
                            default:
                                throw new IllegalStateException("Current state is " + state);
                        }
                    } else if (state == State.CACHING && wrapper.isAwaitingResult()) {
                        if (failure != null) {
                            wrapper.subscriber.onFailure(currentFailure);
                        } else {
                            wrapper.subscriber.onItem(currentItem);
                        }
                        subscribers.remove(wrapper);
                    }
                }
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    @Override
    public void onItem(I item) {
        if (state.get() == State.SUBSCRIBED) {
            this.item = item;
            this.failure = null;
            state.set(State.CACHING);
            drain();
        }
    }

    @Override
    public void onFailure(Throwable failure) {
        if (state.get() == State.SUBSCRIBED) {
            this.item = null;
            this.failure = failure;
            state.set(State.CACHING);
            drain();
        }
    }

    private static class UniSubscriberWrapper<I> {

        enum Status {
            AWAITING_SUBSCRIPTION,
            AWAITING_RESULT,
            CANCELLED
        }

        final UniSubscriber<? super I> subscriber;
        final AtomicReference<Status> status = new AtomicReference<>(Status.AWAITING_SUBSCRIPTION);

        UniSubscriberWrapper(UniSubscriber<? super I> subscriber) {
            this.subscriber = subscriber;
        }

        void markCancelled() {
            status.set(Status.CANCELLED);
        }

        void markSubscribed() {
            status.compareAndSet(Status.AWAITING_SUBSCRIPTION, Status.AWAITING_RESULT);
        }

        boolean isCancelled() {
            return status.get() == Status.CANCELLED;
        }

        boolean isAwaitingSubscription() {
            return status.get() == Status.AWAITING_SUBSCRIPTION;
        }

        boolean isAwaitingResult() {
            return status.get() == Status.AWAITING_RESULT;
        }
    }
}
