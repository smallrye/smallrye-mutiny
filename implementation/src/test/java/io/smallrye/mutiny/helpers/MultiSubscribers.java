package io.smallrye.mutiny.helpers;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiSubscribers {

    private MultiSubscribers() {
        // Avoid direct instantiation.
    }

    public static <T> MultiSubscriber<T> toMultiSubscriber(Subscriber<T> subscriber) {
        return new MultiSubscriberWrapper<>(subscriber);
    }

    private static class MultiSubscriberWrapper<T> implements MultiSubscriber<T>, ContextSupport {

        private final Subscriber<T> delegate;

        private MultiSubscriberWrapper(Subscriber<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Context context() {
            if (delegate instanceof ContextSupport) {
                return ((ContextSupport) delegate).context();
            } else {
                return Context.empty();
            }
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
