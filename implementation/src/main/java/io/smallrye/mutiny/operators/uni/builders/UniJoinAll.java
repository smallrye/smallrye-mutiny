package io.smallrye.mutiny.operators.uni.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniJoinAll<T> extends AbstractUni<List<T>> {

    private final List<Uni<? extends T>> unis;

    public UniJoinAll(List<Uni<? extends T>> unis) {
        this.unis = unis;
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
        private final AtomicInteger itemsCount = new AtomicInteger();

        public UniJoinAllSubscription(UniSubscriber<? super List<T>> subscriber) {
            this.subscriber = subscriber;
        }

        public void triggerSubscriptions() {
            for (int i = 0; i < unis.size() && !cancelled.get(); i++) {
                int index = i;
                Uni<? extends T> uni = unis.get(i);
                uni.onSubscription()
                        .invoke(subscription -> subscriptions.set(index, subscription))
                        .subscribe().with(item -> this.onItem(index, item), this::onFailure);
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

        private void onItem(int index, T item) {
            if (!cancelled.get()) {
                items.set(index, item);
                if (itemsCount.incrementAndGet() == unis.size()) {
                    cancelled.set(true);
                    ArrayList<T> result = new ArrayList<>(unis.size());
                    for (int i = 0; i < unis.size(); i++) {
                        result.add(items.get(i));
                    }
                    subscriber.onItem(result);
                }
            }
        }

        private void onFailure(Throwable failure) {
            if (cancelled.compareAndSet(false, true)) {
                cancelSubscriptions();
                subscriber.onFailure(failure);
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }
    }
}
