package io.smallrye.reactive.unimulti.operators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactivestreams.Subscription;

import io.smallrye.reactive.unimulti.CompositeException;
import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.helpers.EmptyUniSubscription;
import io.smallrye.reactive.unimulti.subscription.UniSubscriber;
import io.smallrye.reactive.unimulti.subscription.UniSubscription;

public class UniAndCombination<I, O> extends UniOperator<I, O> {

    private static final Object SENTINEL = new Object();

    private final Function<List<?>, O> combinator;
    private final List<Uni<?>> unis;
    private final boolean collectAllFailureBeforeFiring;

    public UniAndCombination(Uni<? extends I> upstream, List<? extends Uni<?>> others,
            Function<List<?>, O> combinator,
            boolean collectAllFailureBeforeFiring) {
        super(upstream);

        this.unis = new ArrayList<>();
        // upstream can be null when using the all (static) operator.
        if (upstream != null) {
            this.unis.add(upstream);
        }
        this.unis.addAll(others);

        this.combinator = combinator;
        this.collectAllFailureBeforeFiring = collectAllFailureBeforeFiring;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        AndSupervisor andSupervisor = new AndSupervisor(subscriber);
        subscriber.onSubscribe(andSupervisor);
        // Must wait until the subscriber get a subscription before subscribing to the sources.
        andSupervisor.run();
    }

    private class AndSupervisor implements UniSubscription {

        private final List<UniHandler> handlers = new ArrayList<>();
        private final UniSerializedSubscriber<? super O> subscriber;

        AtomicBoolean cancelled = new AtomicBoolean();

        AndSupervisor(UniSerializedSubscriber<? super O> sub) {
            subscriber = sub;

            for (Uni u : unis) {
                UniHandler result = new UniHandler(this, u);
                handlers.add(result);
            }

        }

        private void run() {
            handlers.forEach(UniHandler::subscribe);
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                handlers.forEach(UniHandler::cancel);
            }
        }

        /**
         * A uni has fired an event (an item or a failure)
         * Checks the progress and decide if an event need to be fired downstream.
         *
         * @param res the {@link UniHandler}
         * @param failed whether the {@code res} just fired a failure
         */
        void check(UniHandler res, boolean failed) {
            int incomplete = unis.size();

            // One of the uni failed, and we can fire a failure immediately.
            if (failed && !collectAllFailureBeforeFiring) {
                if (cancelled.compareAndSet(false, true)) {
                    // Cancel all subscriptions
                    handlers.forEach(UniHandler::cancel);
                    // Invoke observer
                    subscriber.onFailure(res.failure);
                }
                return;
            }

            for (UniHandler result : handlers) {
                if (result.failure != null || result.item != SENTINEL) {
                    incomplete = incomplete - 1;
                }
            }

            if (incomplete == 0) {
                // All unis has fired an event, check the outcome
                if (cancelled.compareAndSet(false, true)) {
                    List<Throwable> failures = getFailures();
                    List<Object> items = getItems();
                    computeAndFireTheOutcome(failures, items);
                }
            }

        }

        private void computeAndFireTheOutcome(List<Throwable> failures, List<Object> items) {
            if (failures.isEmpty()) {
                O aggregated;
                try {
                    aggregated = combinator.apply(items);
                } catch (Exception e) {
                    subscriber.onFailure(e);
                    return;
                }
                subscriber.onItem(aggregated);
            } else if (failures.size() == 1) {
                // If we had a single failure, fire it without the CompositeException envelope.
                subscriber.onFailure(failures.get(0));
            } else {
                subscriber.onFailure(new CompositeException(failures));
            }
        }

        private List<Object> getItems() {
            return this.handlers.stream()
                    .map(u -> u.item)
                    .collect(Collectors.toList());
        }

        private List<Throwable> getFailures() {
            return handlers.stream()
                    .filter(u -> u.failure != null).map(u -> u.failure)
                    .collect(Collectors.toList());
        }
    }

    private class UniHandler implements UniSubscription, UniSubscriber {

        final AtomicReference<UniSubscription> subscription = new AtomicReference<>();
        private final AndSupervisor supervisor;
        private final Uni uni;
        Object item = SENTINEL;
        Throwable failure;

        UniHandler(AndSupervisor supervisor, Uni observed) {
            this.supervisor = supervisor;
            this.uni = observed;
        }

        @Override
        public final void onSubscribe(UniSubscription sub) {
            if (!subscription.compareAndSet(null, sub)) {
                // cancelling this second subscription
                // because we already add a subscription (most probably CANCELLED)
                sub.cancel();
            }
        }

        @Override
        public final void onFailure(Throwable t) {
            if (subscription.getAndSet(EmptyUniSubscription.CANCELLED) == EmptyUniSubscription.CANCELLED) {
                // Already cancelled, do nothing
                return;
            }
            this.failure = t;
            supervisor.check(this, true);
        }

        @Override
        public final void onItem(Object x) {
            if (subscription.getAndSet(EmptyUniSubscription.CANCELLED) == EmptyUniSubscription.CANCELLED) {
                // Already cancelled, do nothing
                return;
            }
            this.item = x;
            supervisor.check(this, false);
        }

        @Override
        public void cancel() {
            Subscription sub = subscription.getAndSet(EmptyUniSubscription.CANCELLED);
            if (sub != null) {
                sub.cancel();
            }
        }

        public void subscribe() {
            uni.subscribe().withSubscriber(this);
        }
    }
}
