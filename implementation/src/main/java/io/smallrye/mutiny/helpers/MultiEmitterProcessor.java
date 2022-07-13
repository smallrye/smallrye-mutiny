package io.smallrye.mutiny.helpers;

import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiEmitterProcessor<T> implements Processor<T, T>, MultiEmitter<T> {

    private final UnicastProcessor<T> processor;
    private final AtomicReference<Runnable> onTermination = new AtomicReference<>();
    private final AtomicBoolean terminated = new AtomicBoolean();
    private final AtomicLong requested = new AtomicLong();

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

    @Override
    public boolean isCancelled() {
        return terminated.get();
    }

    @Override
    public long requested() {
        return requested.get();
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
                        Subscriptions.add(requested, l);
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
        if (terminated.compareAndSet(false, true)) {
            Runnable runnable = onTermination.getAndSet(null);
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        processor.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        Subscriptions.subtract(requested, 1);
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

    @Override
    public Context context() {
        throw new UnsupportedOperationException("This class is used in tests");
    }
}
