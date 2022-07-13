package io.smallrye.mutiny.streams.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.StrictMultiSubscriber;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Processor wrapping a publisher and subscriber, and connect them
 */
public class WrappedProcessor<T> implements Processor<T, T> {
    private final Subscriber<T> subscriber;
    private final Publisher<T> publisher;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    public WrappedProcessor(Subscriber<T> subscriber, Publisher<T> publisher) {
        this.subscriber = subscriber;
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        publisher.subscribe(
                AdaptersToReactiveStreams.subscriber(new StrictMultiSubscriber<>(AdaptersToFlow.subscriber(subscriber))));
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        Objects.requireNonNull(subscription);
        if (!subscribed.compareAndSet(false, true)) {
            subscription.cancel();
        } else {
            subscriber.onSubscribe(subscription);
        }
    }

    @Override
    public void onNext(T item) {
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(Objects.requireNonNull(throwable));
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
