package io.smallrye.mutiny.streams.utils;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;

public class DefaultSubscriberWithCompletionStage<T, R> implements SubscriberWithCompletionStage<T, R> {
    private final CompletionSubscriber<T, R> subscriber;

    public DefaultSubscriberWithCompletionStage(Processor<T, T> processor, CompletionStage<R> result) {
        subscriber = CompletionSubscriber.of(processor, result);
    }

    @Override
    public CompletionStage<R> getCompletion() {
        return subscriber.getCompletion();
    }

    @Override
    public Subscriber<T> getSubscriber() {
        return subscriber;
    }
}
