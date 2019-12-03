package io.smallrye.mutiny.streams.utils;

import java.util.Objects;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CancellationSubscriber<T> implements Subscriber<T> {

    @Override
    public void onSubscribe(Subscription s) {
        Objects.requireNonNull(s).cancel();
    }

    @Override
    public void onNext(T t) {
        // Just check for null value.
        Objects.requireNonNull(t);
    }

    @Override
    public void onError(Throwable t) {
        // Ignored.
        Objects.requireNonNull(t);
    }

    @Override
    public void onComplete() {
        // Ignored.
    }
}
