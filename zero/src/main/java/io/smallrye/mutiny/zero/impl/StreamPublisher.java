package io.smallrye.mutiny.zero.impl;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class StreamPublisher<T> implements Publisher<T> {

    private final Supplier<Stream<T>> supplier;

    public StreamPublisher(Supplier<Stream<T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        Stream<T> stream = supplier.get();
        if (stream == null) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            subscriber.onError(new NullPointerException("The supplied stream cannot be null"));
        } else {
            subscriber.onSubscribe(new IteratorSubscription<>(stream.iterator(), subscriber));
        }
    }
}
