package io.smallrye.mutiny.operators.uni.builders;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniJoinFirst<T> extends AbstractUni<T> {

    private final List<Uni<? extends T>> unis;

    public UniJoinFirst(List<Uni<? extends T>> unis) {
        this.unis = unis;
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

        public UniJoinFirstSubscription(UniSubscriber<? super T> subscriber) {
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
            if (cancelled.compareAndSet(false, true)) {
                cancelSubscriptions();
                subscriber.onItem(item);
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
