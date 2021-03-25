package io.smallrye.mutiny.zero.impl;

import static java.util.Objects.requireNonNull;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class IterablePublisher<T> implements Publisher<T> {

    private final Iterable<T> iterable;

    public IterablePublisher(Iterable<T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        if (!iterable.iterator().hasNext()) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            subscriber.onComplete();
        } else {
            subscriber.onSubscribe(new IteratorSubscription(iterable.iterator(), subscriber));
        }
    }

}
