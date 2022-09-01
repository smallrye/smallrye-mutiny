package io.smallrye.mutiny.jakarta.streams.utils;

import java.util.Objects;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.subscription.SafeSubscriber;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

@SuppressWarnings({ "PublisherImplementation", "ReactiveStreamsPublisherImplementation" })
public class CouplingProcessor<I, O> implements Publisher<O> {

    private final SubscriptionObserver<I> controller;
    private final Publisher<O> publisher;

    public CouplingProcessor(Publisher<I> source, Subscriber<I> subscriber, Publisher<O> publisher) {
        Objects.requireNonNull(subscriber);
        controller = new SubscriptionObserver<>(source, subscriber);
        this.publisher = publisher;
        controller.run();
    }

    @Override
    public synchronized void subscribe(Subscriber<? super O> subscriber) {
        Objects.requireNonNull(subscriber);
        SubscriptionObserver<O> observer = new SubscriptionObserver<>(this.publisher,
                AdaptersToReactiveStreams.subscriber(new SafeSubscriber<>(AdaptersToFlow.subscriber(subscriber))));
        controller.setObserver(observer);
        observer.setObserver(controller);
        observer.run();
    }

}
