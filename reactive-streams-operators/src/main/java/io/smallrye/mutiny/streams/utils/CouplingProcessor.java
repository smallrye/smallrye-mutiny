package io.smallrye.mutiny.streams.utils;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.subscription.SafeSubscriber;

@SuppressWarnings("PublisherImplementation")
public class CouplingProcessor<I, O> implements Publisher<O> {

    private final SubscriptionObserver<I> controller;
    private final Publisher<O> publisher;

    public CouplingProcessor(Publisher<I> source, Subscriber<I> subscriber, Publisher<O> publisher) {
        controller = new SubscriptionObserver<>(source, subscriber);
        this.publisher = publisher;
        controller.run();
    }

    @Override
    public synchronized void subscribe(Subscriber<? super O> subscriber) {
        SubscriptionObserver<O> observer = new SubscriptionObserver<>(this.publisher, new SafeSubscriber<>(subscriber));
        controller.setObserver(observer);
        observer.setObserver(controller);
        observer.run();
    }

}
