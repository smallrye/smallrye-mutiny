package io.smallrye.mutiny.operators.uni.builders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniJoinFirst<T> extends AbstractUni<T> {

    public enum Mode {
        FIRST_TO_EMIT,
        FIRST_WITH_ITEM
    }

    private final List<Uni<T>> unis;
    private final Mode mode;
    private final int concurrency;

    public UniJoinFirst(List<Uni<T>> unis, Mode mode, int concurrency) {
        this.unis = unis;
        this.mode = mode;
        this.concurrency = concurrency;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        UniJoinFirstSubscription joinAllSubscription = new UniJoinFirstSubscription(subscriber);
        subscriber.onSubscribe(joinAllSubscription);
        joinAllSubscription.triggerSubscriptions();
    }

    private class UniJoinFirstSubscription implements UniSubscription {

        private final UniSubscriber<? super T> subscriber;

        private final AtomicReferenceArray<UniSubscription> subscriptions = new AtomicReferenceArray<>(unis.size());
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private AtomicInteger nextSubscriptionIndex;

        public UniJoinFirstSubscription(UniSubscriber<? super T> subscriber) {
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
            for (int i = 0; i < limit && !cancelled.get(); i++) {
                performSubscription(i);
            }
        }

        private void performSubscription(int index) {
            Uni<? extends T> uni = unis.get(index);
            uni.onSubscription()
                    .invoke(subscription -> this.onSubscribe(index, subscription))
                    .subscribe().with(subscriber.context(), this::onItem, this::onFailure);
        }

        private void onSubscribe(int index, UniSubscription subscription) {
            if (!cancelled.get()) {
                subscriptions.set(index, subscription);
            } else {
                subscription.cancel();
            }
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

        private void onItem(T item) {
            if (cancelled.compareAndSet(false, true)) {
                cancelSubscriptions();
                subscriber.onItem(item);
            }
        }

        private final List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        private void onFailure(Throwable failure) {
            switch (mode) {
                case FIRST_TO_EMIT:
                    if (cancelled.compareAndSet(false, true)) {
                        cancelSubscriptions();
                        subscriber.onFailure(failure);
                    } else {
                        Infrastructure.handleDroppedException(failure);
                    }
                    break;
                case FIRST_WITH_ITEM:
                    if (!cancelled.get()) {
                        failures.add(failure);
                        if (failures.size() == unis.size()) {
                            cancelled.set(true);
                            subscriber.onFailure(new CompositeException(failures));
                        } else if (concurrency != -1) {
                            int nextIndex = nextSubscriptionIndex.incrementAndGet();
                            if (nextIndex < unis.size()) {
                                performSubscription(nextIndex);
                            }
                        }
                    } else {
                        Infrastructure.handleDroppedException(failure);
                    }
                    break;
            }
        }
    }
}
