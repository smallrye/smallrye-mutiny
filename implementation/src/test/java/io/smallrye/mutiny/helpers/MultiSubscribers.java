package io.smallrye.mutiny.helpers;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiSubscribers {

    private MultiSubscribers() {
        // Avoid direct instantiation.
    }

    public static <T> MultiSubscriber<T> toMultiSubscriber(Subscriber<T> subscriber) {
        return new MultiSubscriberWrapper<>(subscriber);
    }

    private static class MultiSubscriberWrapper<T> implements MultiSubscriber<T> {

        private final Subscriber<T> delegate;

        private MultiSubscriberWrapper(Subscriber<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onItem(T item) {
            delegate.onNext(item);
        }

        @Override
        public void onFailure(Throwable failure) {
            delegate.onError(failure);
        }

        @Override
        public void onCompletion() {
            delegate.onComplete();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            delegate.onSubscribe(subscription);
        }
    }
}
