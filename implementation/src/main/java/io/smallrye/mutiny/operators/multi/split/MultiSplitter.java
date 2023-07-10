package io.smallrye.mutiny.operators.multi.split;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Splits a {@link Multi} into several co-operating {@link Multi}.
 * <p>
 * Each split {@link Multi} receives items based on a function that maps each item to a key from an enumeration.
 * <p>
 * The demand of each split {@link Multi} is independent.
 * Items flow when all keys from the enumeration have a split subscriber, and until either one of the split has a {@code 0}
 * demand,
 * or when one of the split subscriber cancels.
 * The flow resumes when all keys have a subscriber again, and when the demand for each split is strictly positive.
 * <p>
 * Calls to {@link #get(Enum)} result in new {@link Multi} objects, but given a key {@code K} then there can be only one
 * active subscription. If there is already a subscriber for {@code K} then any subscription request to a {@link Multi} for key
 * {@code K} results in a terminal failure.
 * Note that when a subscriber for {@code K} has cancelled then a request to subscribe for a {@link Multi} for {@code K} can
 * succeed.
 * <p>
 * If the upstream {@link Multi} has already completed or failed, then any new subscriber will receive the terminal signal
 * (see {@link MultiSubscriber#onCompletion()} and {@link MultiSubscriber#onFailure(Throwable)}).
 * <p>
 * Note on {@link Context} support: it is assumed that all split subscribers share the same {@link Context} instance, if any.
 * The {@link Context} is passed to the upstream {@link Multi} when the first split subscription happens.
 * When disjoint {@link Context} are in use by the different split subscribers then the behavior of your code will be most
 * likely incorrect.
 *
 * @param <T> the items type
 * @param <K> the enumeration type
 */
public class MultiSplitter<T, K extends Enum<K>> {

    private final Multi<? extends T> upstream;
    private final Function<T, K> splitter;
    private final ConcurrentHashMap<K, SplitMulti.Split> splits;
    private final int requiredNumberOfSubscribers;
    private final Class<K> keyType;

    public MultiSplitter(Multi<? extends T> upstream, Class<K> keyType, Function<T, K> splitter) {
        this.upstream = nonNull(upstream, "upstream");
        if (!nonNull(keyType, "keyType").isEnum()) {
            // Note: the Java compiler enforces a type check on keyType being some enum, so this branch is only here for added peace of mind
            throw new IllegalArgumentException("The key type must be that of an enumeration");
        }
        this.keyType = keyType;
        this.splitter = nonNull(splitter, "splitter");
        this.splits = new ConcurrentHashMap<>();
        this.requiredNumberOfSubscribers = keyType.getEnumConstants().length;
    }

    /**
     * Get a {@link Multi} for a given key.
     *
     * @param key the key
     * @return a new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> get(K key) {
        return Infrastructure.onMultiCreation(new SplitMulti(key));
    }

    /**
     * Get the (enum) key type.
     *
     * @return the key type
     */
    public Class<K> keyType() {
        return keyType;
    }

    private enum State {
        INIT,
        AWAITING_SUBSCRIPTION,
        SUBSCRIBED,
        COMPLETED,
        FAILED
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

    private volatile Throwable terminalFailure;

    private Flow.Subscription upstreamSubscription;

    private void onSplitRequest() {
        if (state.get() != State.SUBSCRIBED || splits.size() < requiredNumberOfSubscribers) {
            return;
        }
        for (SplitMulti.Split split : splits.values()) {
            if (split.demand.get() == 0L) {
                return;
            }
        }
        upstreamSubscription.request(1L);
    }

    private void onUpstreamFailure() {
        for (SplitMulti.Split split : splits.values()) {
            split.downstream.onFailure(terminalFailure);
        }
        splits.clear();
    }

    private void onUpstreamCompletion() {
        for (SplitMulti.Split split : splits.values()) {
            split.downstream.onCompletion();
        }
        splits.clear();
    }

    private void onUpstreamItem(T item) {
        try {
            K key = splitter.apply(item);
            if (key == null) {
                throw new NullPointerException("The splitter function returned null");
            }
            // Note: if the target subscriber was removed between the last upstream demand and now, it is simply discarded
            SplitMulti.Split target = splits.get(key);
            if (target != null) {
                target.downstream.onItem(item);
                if (splits.size() == requiredNumberOfSubscribers
                        && (target.demand.get() == Long.MAX_VALUE || target.demand.decrementAndGet() > 0L)) {
                    upstreamSubscription.request(1L);
                }
            }
        } catch (Throwable err) {
            terminalFailure = err;
            state.set(State.FAILED);
            onUpstreamFailure();
        }
    }

    // Note: we need a subscriber class because another onCompletion definition exists in Multi
    private class Forwarder implements MultiSubscriber<T>, ContextSupport {

        private final Context context;

        private Forwarder(MultiSubscriber<? super T> firstSubscriber) {
            if (firstSubscriber instanceof ContextSupport) {
                context = ((ContextSupport) firstSubscriber).context();
            } else {
                context = Context.empty();
            }
        }

        @Override
        public void onItem(T item) {
            if (state.get() != State.SUBSCRIBED) {
                return;
            }
            onUpstreamItem(item);
        }

        @Override
        public void onFailure(Throwable failure) {
            if (state.compareAndSet(State.SUBSCRIBED, State.FAILED)) {
                terminalFailure = failure;
                onUpstreamFailure();
            }
        }

        @Override
        public void onCompletion() {
            if (state.compareAndSet(State.SUBSCRIBED, State.COMPLETED)) {
                onUpstreamCompletion();
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (state.get() != State.AWAITING_SUBSCRIPTION) {
                subscription.cancel();
            } else {
                upstreamSubscription = subscription;
                state.set(State.SUBSCRIBED);
                // In case all splits would be subscribed...
                onSplitRequest();
            }
        }

        @Override
        public Context context() {
            return context;
        }
    }

    private class SplitMulti extends AbstractMulti<T> {

        private final K key;

        private SplitMulti(K key) {
            this.key = key;
        }

        @Override
        public void subscribe(MultiSubscriber<? super T> subscriber) {
            nonNull(subscriber, "subscriber");

            // First subscription triggers upstream subscription
            if (state.compareAndSet(State.INIT, State.AWAITING_SUBSCRIPTION)) {
                // Assumption: all split subscribers share the same context, if any
                upstream.subscribe().withSubscriber(new Forwarder(subscriber));
            }

            // Early exits
            State stateWhenSubscribing = state.get();
            if (stateWhenSubscribing == State.FAILED) {
                subscriber.onSubscribe(Subscriptions.CANCELLED);
                subscriber.onFailure(terminalFailure);
                return;
            }
            if (stateWhenSubscribing == State.COMPLETED) {
                subscriber.onSubscribe(Subscriptions.CANCELLED);
                subscriber.onCompletion();
                return;
            }

            // Regular subscription path
            Split split = new Split(subscriber);
            Split previous = splits.putIfAbsent(key, split);
            if (previous == null) {
                subscriber.onSubscribe(split);
            } else {
                subscriber.onSubscribe(Subscriptions.CANCELLED);
                subscriber.onError(new IllegalStateException("There is already a subscriber for key " + key));
            }
        }

        private class Split implements Flow.Subscription {

            MultiSubscriber<? super T> downstream;
            AtomicLong demand = new AtomicLong();

            private Split(MultiSubscriber<? super T> subscriber) {
                this.downstream = subscriber;
            }

            @Override
            public void request(long n) {
                if (n <= 0) {
                    cancel();
                    downstream.onError(Subscriptions.getInvalidRequestException());
                    return;
                }
                Subscriptions.add(demand, n);
                onSplitRequest();
            }

            @Override
            public void cancel() {
                splits.remove(key);
            }
        }
    }
}
