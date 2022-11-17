package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;

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
    public void subscribe(UniSubscriber<? super T> subscriber) {
        new PublisherSubscriber(subscriber).forward();
    }

    private class PublisherSubscriber implements UniSubscription, Flow.Subscriber<T>, ContextSupport {

        private final UniSubscriber<? super T> subscriber;
        AtomicReference<Subscription> subscription = new AtomicReference<>();

        private PublisherSubscriber(UniSubscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        private void forward() {
            subscriber.onSubscribe(this);
            Flow.Subscriber<? super T> sub = Infrastructure.onMultiSubscription(publisher, this);
            publisher.subscribe(sub);
        }

        // ---- UniSubscription

        @Override
        public void cancel() {
            Subscription old = subscription.getAndSet(CANCELLED);
            if (old != null) {
                old.cancel();
            }
        }

        // ---- Subscriber

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription.compareAndSet(null, s)) {
                s.request(1L);
            } else {
                s.cancel();
            }
        }

        @Override
        public void onNext(T item) {
            Subscription sub = subscription.getAndSet(CANCELLED);
            if (sub != CANCELLED) {
                sub.cancel();
                subscriber.onItem(item);
            }
        }

        @Override
        public void onError(Throwable failure) {
            Subscription sub = subscription.getAndSet(CANCELLED);
            if (sub != CANCELLED) {
                subscriber.onFailure(failure);
            }
        }

        @Override
        public void onComplete() {
            Subscription sub = subscription.getAndSet(CANCELLED);
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
