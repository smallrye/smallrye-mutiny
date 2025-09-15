package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCreateFromPublisher<T> extends AbstractUni<T> {
    private final Flow.Publisher<? extends T> publisher;

    public UniCreateFromPublisher(Flow.Publisher<? extends T> publisher) {
        this.publisher = nonNull(publisher, "publisher");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(UniSubscriber<? super T> subscriber) {
        new PublisherSubscriber(publisher, subscriber).forward();
    }

    private static class PublisherSubscriber<T> implements UniSubscription, Flow.Subscriber<T>, ContextSupport {

        private final UniSubscriber<? super T> subscriber;
        private final Flow.Publisher<? extends T> publisher;

        private volatile Subscription subscription;
        private static final AtomicReferenceFieldUpdater<PublisherSubscriber, Subscription> SUBSCRIPTION_UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(PublisherSubscriber.class, Subscription.class, "subscription");

        private PublisherSubscriber(Flow.Publisher<? extends T> publisher, UniSubscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            this.publisher = publisher;
        }

        private void forward() {
            subscriber.onSubscribe(this);
            Flow.Subscriber<? super T> sub = Infrastructure.onMultiSubscription(publisher, this);
            publisher.subscribe(sub);
        }

        // ---- UniSubscription

        @Override
        public void cancel() {
            Subscription old = SUBSCRIPTION_UPDATER.getAndSet(this, CANCELLED);
            if (old != null) {
                old.cancel();
            }
        }

        // ---- Subscriber

        @Override
        public void onSubscribe(Subscription sub) {
            if (SUBSCRIPTION_UPDATER.compareAndSet(this, null, sub)) {
                sub.request(1L);
            } else {
                sub.cancel();
            }
        }

        @Override
        public void onNext(T item) {
            Subscription sub = SUBSCRIPTION_UPDATER.getAndSet(this, CANCELLED);
            if (sub != CANCELLED) {
                sub.cancel();
                subscriber.onItem(item);
            }
        }

        @Override
        public void onError(Throwable failure) {
            Subscription sub = SUBSCRIPTION_UPDATER.getAndSet(this, CANCELLED);
            if (sub != CANCELLED) {
                subscriber.onFailure(failure);
            }
        }

        @Override
        public void onComplete() {
            Subscription sub = SUBSCRIPTION_UPDATER.getAndSet(this, CANCELLED);
            if (sub != CANCELLED) {
                subscriber.onItem(null);
            }
        }

        @Override
        public Context context() {
            return subscriber.context();
        }
    }
}
