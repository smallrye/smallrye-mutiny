package io.smallrye.mutiny.helpers;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiEmitterProcessor<T> implements Processor<T, T>, MultiEmitter<T> {

    private final UnicastProcessor<T> processor;
    private final AtomicReference<Runnable> onTermination = new AtomicReference<>();

    private MultiEmitterProcessor() {
        this.processor = UnicastProcessor.create();
    }

    public static <T> MultiEmitterProcessor<T> create() {
        return new MultiEmitterProcessor<>();
    }

    @Override
    public MultiEmitter<T> emit(T item) {
        onNext(item);
        return this;
    }

    @Override
    public void fail(Throwable failure) {
        onError(failure);
    }

    @Override
    public void complete() {
        onComplete();
    }

    @Override
    public MultiEmitter<T> onTermination(Runnable onTermination) {
        this.onTermination.set(onTermination);
        return this;
    }

    @SuppressWarnings("SubscriberImplementation")
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        processor.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long l) {
                        subscription.request(l);
                    }

                    @Override
                    public void cancel() {
                        subscription.cancel();
                        fireTermination();
                    }
                });
            }

            @Override
            public void onNext(T item) {
                subscriber.onNext(item);
            }

            @Override
            public void onError(Throwable failure) {
                subscriber.onError(failure);
                fireTermination();
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
                fireTermination();
            }
        });
    }

    private void fireTermination() {
        Runnable runnable = onTermination.getAndSet(null);
        if (runnable != null) {
            runnable.run();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        processor.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        processor.onNext(item);
    }

    @Override
    public void onError(Throwable failure) {
        processor.onError(failure);
    }

    @Override
    public void onComplete() {
        processor.onComplete();
    }

    public Multi<T> toMulti() {
        return Multi.createFrom().publisher(this);
    }
}
