package io.smallrye.mutiny.operators.uni.builders;

import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the item is known.
 * The item can be {@code null}.
 *
 * @param <T> the type of the item
 */
public class UniCreateFromKnownItem<T> extends AbstractUni<T> {

    private final T item;

    public UniCreateFromKnownItem(T item) {
        this.item = item;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        new KnownItemSubscription(subscriber).forward();
    }

    private class KnownItemSubscription implements UniSubscription {

        private final UniSubscriber<? super T> subscriber;
        private volatile boolean cancelled = false;

        private KnownItemSubscription(UniSubscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        private void forward() {
            subscriber.onSubscribe(this);
            if (!cancelled) {
                subscriber.onItem(item);
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
