package io.smallrye.reactive.unimulti.helpers;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@SuppressWarnings("SubscriberImplementation")
public class CancellationSubscriber<T> implements Subscriber<T> {
    @Override
    public void onSubscribe(Subscription s) {
        s.cancel();
    }

    @Override
    public void onNext(T t) {
        // Ignored
    }

    @Override
    public void onError(Throwable t) {
        // Ignored
    }

    @Override
    public void onComplete() {
        // Ignored
    }
}
