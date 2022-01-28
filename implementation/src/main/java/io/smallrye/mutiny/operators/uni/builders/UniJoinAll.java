package io.smallrye.mutiny.operators.uni.builders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniJoinAll<T> extends AbstractUni<List<T>> {

    public enum Mode {
        COLLECT_FAILURES,
        FAIL_FAST
    }

    private final List<Uni<T>> unis;
    private final Mode mode;
    private final int concurrency;

    public UniJoinAll(List<Uni<T>> unis, Mode mode, int concurrency) {
        this.unis = unis;
        this.mode = mode;
        this.concurrency = concurrency;
    }

    @Override
    public void subscribe(UniSubscriber<? super List<T>> subscriber) {
        UniJoinAllSubscription joinAllSubscription = new UniJoinAllSubscription(subscriber);
        subscriber.onSubscribe(joinAllSubscription);
        joinAllSubscription.triggerSubscriptions();
    }

    private class UniJoinAllSubscription implements UniSubscription {

        private final UniSubscriber<? super List<T>> subscriber;

        private final AtomicReferenceArray<UniSubscription> subscriptions = new AtomicReferenceArray<>(unis.size());
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private final AtomicReferenceArray<T> items = new AtomicReferenceArray<>(unis.size());
        private final List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger completionSignalsCount = new AtomicInteger();

        private AtomicInteger nextSubscriptionIndex;

        public UniJoinAllSubscription(UniSubscriber<? super List<T>> subscriber) {
            this.subscriber = subscriber;
        }

        public void triggerSubscriptions() {
            int limit;
            if (concurrency != -1) {
                limit = Math.min(concurrency, unis.size());
                nextSubscriptionIndex = new AtomicInteger(concurrency - 1);
            } else {
                limit = unis.size();
            }
            for (int index = 0; index < limit; index++) {
                if (!trySubscribe(index, unis.get(index))) {
                    break;
                }
            }
        }

        private boolean trySubscribe(int index, Uni<? extends T> uni) {
            boolean proceed = !this.cancelled.get();
            if (proceed) {
                uni.onSubscription()
                        .invoke(subscription -> this.onSubscribe(index, subscription))
                        .subscribe().with(subscriber.context(), item -> this.onItem(index, item), this::onFailure);
            }
            return proceed;
        }

        @Override
        public void cancel() {
            cancelled.set(true);
            cancelSubscriptions();
        }

        private void cancelSubscriptions() {
            for (int i = 0; i < unis.size(); i++) {
                UniSubscription sub = subscriptions.get(i);
                if (sub != null) {
                    sub.cancel();
                }
            }
        }

        private void onSubscribe(int index, UniSubscription subscription) {
            if (!cancelled.get()) {
                subscriptions.set(index, subscription);
            } else {
                subscription.cancel();
            }
        }

        private void onItem(int index, T item) {
            if (!cancelled.get()) {
                items.set(index, item);
                forwardSignalWhenCompleteOrSubscribeNext();
            }
        }

        private void forwardSignalWhenCompleteOrSubscribeNext() {
            if (completionSignalsCount.incrementAndGet() == unis.size()) {
                cancelled.set(true);
                if (failures.isEmpty()) {
                    ArrayList<T> result = new ArrayList<>(unis.size());
                    for (int i = 0; i < unis.size(); i++) {
                        result.add(items.get(i));
                    }
                    subscriber.onItem(result);
                } else {
                    subscriber.onFailure(new CompositeException(failures));
                }
            } else if (concurrency != -1) {
                int nextIndex = nextSubscriptionIndex.incrementAndGet();
                if (nextIndex < unis.size()) {
                    trySubscribe(nextIndex, unis.get(nextIndex));
                }
            }
        }

        private void onFailure(Throwable failure) {
            switch (mode) {
                case COLLECT_FAILURES:
                    if (!cancelled.get()) {
                        failures.add(failure);
                        forwardSignalWhenCompleteOrSubscribeNext();
                    }
                    break;
                case FAIL_FAST:
                    if (cancelled.compareAndSet(false, true)) {
                        cancelSubscriptions();
                        subscriber.onFailure(failure);
                    }
                    break;
            }
        }
    }
}
