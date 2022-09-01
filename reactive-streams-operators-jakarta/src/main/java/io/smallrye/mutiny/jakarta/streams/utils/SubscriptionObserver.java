package io.smallrye.mutiny.jakarta.streams.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.Subscriptions;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Represents a subscription between a source and a sink. Inform another {@link SubscriptionObserver} on cancellation,
 * completion and error. This
 * class is used to coupled 2 flows (Coupled operator).
 *
 * @param <X> the type of data transiting in the stream
 */
class SubscriptionObserver<X> {

    /**
     * The state ot the exchange.
     */
    private enum State {
        /**
         * Initialization - no subscriber yet
         */
        INIT,
        /**
         * A subscriber is observing, the subscription has been passed to this subscriber.
         */
        SUBSCRIBED,
        /**
         * The stream has completed.
         */
        COMPLETED,
        /**
         * The stream has failed.
         */
        FAILED
    }

    /**
     * The source.
     */
    private final Publisher<X> upstream;

    /**
     * The sink.
     * It's an atomic reference to allow releasing the reference on the subscriber as mandated by the
     * Reactive Streams TCK.
     */
    private final AtomicReference<Subscriber<? super X>> downstream = new AtomicReference<>();

    /**
     * A reference on another {@link SubscriptionObserver} notified on cancellation, termination and
     * failure.
     */
    private final AtomicReference<SubscriptionObserver> observer = new AtomicReference<>();

    /**
     * The current state.
     */
    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

    /**
     * The subscription to upstream.
     */
    private final AtomicReference<Subscription> subscription = new AtomicReference<>();

    /**
     * A reference on the error emitted by upstream (if any). It's required as the observer can
     * start the observation after the error to be thrown and so we must pass it.
     */
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    SubscriptionObserver(Publisher<X> upstream, Subscriber<? super X> downstream) {
        this.upstream = Objects.requireNonNull(upstream);
        this.downstream.set(Objects.requireNonNull(downstream));
    }

    void setObserver(SubscriptionObserver other) {
        this.observer.set(Objects.requireNonNull(other));
    }

    @SuppressWarnings("SubscriberImplementation")
    public void run() {
        upstream.subscribe(new Subscriber<X>() {
            @Override
            public synchronized void onSubscribe(Subscription sub) {
                if (manageCompletionOrErrorFromBeforeSubscription(sub) || manageAlreadySubscribed(sub)) {
                    return;
                }
                if (downstream.get() == null) {
                    sub.cancel();
                }
                // The subscription is set.
                state.set(State.SUBSCRIBED);
                injectSubscription(downstream.get(), sub);
            }

            @Override
            public void onNext(X o) {
                apply(downstream, d -> d.onNext(Objects.requireNonNull(o)));
            }

            @Override
            public synchronized void onError(Throwable t) {
                state.set(State.FAILED);
                apply(downstream, d -> d.onError(Objects.requireNonNull(t)));
                new Thread(() -> {
                    apply(subscription, Subscription::cancel);
                }).start();
                failure.set(t);
                apply(observer, obs -> obs.error(t));
            }

            @Override
            public synchronized void onComplete() {
                state.set(State.COMPLETED);
                apply(downstream, Subscriber::onComplete);
                new Thread(() -> {
                    apply(subscription, Subscription::cancel);
                }).start();
                apply(observer, SubscriptionObserver::complete);
            }
        });
    }

    private void injectSubscription(Subscriber<? super X> subscriber, Subscription sub) {
        subscriber.onSubscribe(new WrappedSubscription(sub,
                // On cancellation, notify the observer and cleanup.
                // The delegate subscription is going to be cancelled automatically.
                () -> {
                    if (state.get() == State.SUBSCRIBED || state.get() == State.INIT) {
                        // If we are already in the COMPLETED or FAILED state, stay in the state.
                        state.set(State.COMPLETED);
                    }
                    downstream.set(null);
                    apply(observer, SubscriptionObserver::complete);
                }));
    }

    private boolean manageAlreadySubscribed(Subscription sub) {
        if (!subscription.compareAndSet(null, sub)) {
            sub.cancel();
            return true;
        }
        return false;
    }

    private synchronized Throwable failure() {
        return failure.get();
    }

    private synchronized boolean manageCompletionOrErrorFromBeforeSubscription(Subscription sub) {
        SubscriptionObserver obs = observer.get();
        if (obs != null) {
            if (obs.state.get() == State.FAILED) {
                state.set(State.FAILED);
                apply(downstream, d -> {
                    d.onSubscribe(AdaptersToReactiveStreams.subscription(new Subscriptions.EmptySubscription()));
                    d.onError(obs.failure());
                });
                sub.cancel();
                return true;
            }
            if (obs.state.get() == State.COMPLETED) {
                state.set(State.COMPLETED);
                apply(downstream, d -> {
                    d.onSubscribe(AdaptersToReactiveStreams.subscription(new Subscriptions.EmptySubscription()));
                    d.onComplete();
                });
                sub.cancel();
                return true;
            }
        }
        return false;
    }

    /**
     * The observed streams has failed.
     *
     * @param failure the error.
     */
    private synchronized void error(Throwable failure) {
        if (state.compareAndSet(State.SUBSCRIBED, State.FAILED)) {
            apply(downstream, stream -> stream.onError(failure));
            apply(subscription, Subscription::cancel);
            downstream.set(null);
        }
    }

    public synchronized void complete() {
        if (state.compareAndSet(State.SUBSCRIBED, State.COMPLETED)) {
            apply(downstream, Subscriber::onComplete);
            apply(subscription, Subscription::cancel);
            downstream.set(null);
        }
    }

    private static <X> void apply(AtomicReference<X> ref, Consumer<X> consumer) {
        X x = ref.get();
        if (x != null) {
            consumer.accept(x);
        }
    }
}
