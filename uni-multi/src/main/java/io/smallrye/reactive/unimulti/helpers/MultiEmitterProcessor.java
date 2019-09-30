package io.smallrye.reactive.unimulti.helpers;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.processors.UnicastProcessor;
import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.subscription.MultiEmitter;

public class MultiEmitterProcessor<T> implements Processor<T, T>, MultiEmitter<T> {

    private final UnicastProcessor<T> processor;
    private volatile Runnable onTermination;

    private MultiEmitterProcessor(int bufferSize) {
        this.processor = UnicastProcessor.create(bufferSize);
    }

    public static <T> MultiEmitterProcessor<T> create() {
        return create(128);
    }

    public static <T> MultiEmitterProcessor<T> create(int bufferSize) {
        return new MultiEmitterProcessor<>(bufferSize);
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
        this.onTermination = onTermination;
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
                        if (onTermination != null) {
                            onTermination.run();
                        }
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
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });
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
