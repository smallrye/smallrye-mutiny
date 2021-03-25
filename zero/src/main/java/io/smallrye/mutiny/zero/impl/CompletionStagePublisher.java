package io.smallrye.mutiny.zero.impl;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class CompletionStagePublisher<T> implements Publisher<T> {

    private final CompletableFuture<T> completableFuture;

    public CompletionStagePublisher(CompletionStage<T> completionStage) {
        this.completableFuture = completionStage.toCompletableFuture();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        if (completableFuture.isDone()) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            try {
                T value = completableFuture.get();
                if (value == null) {
                    subscriber.onError(new NullPointerException("The CompletionStage produced a null value"));
                } else {
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }
            } catch (InterruptedException e) {
                subscriber.onError(e);
            } catch (ExecutionException e) {
                subscriber.onError(e.getCause());
            }
        } else {
            subscriber.onSubscribe(new CompletionStageSubscription(subscriber));
        }
    }

    private class CompletionStageSubscription implements Subscription {

        private final Subscriber<? super T> subscriber;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private CompletionStageSubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (cancelled.get()) {
                return;
            }
            if (n <= 0L) {
                cancel();
                subscriber.onError(Helper.negativeRequest(n));
            } else {
                completableFuture.whenComplete((value, err) -> {
                    if (cancelled.compareAndSet(false, true)) {
                        if (err != null) {
                            subscriber.onError(err);
                        } else if (value == null) {
                            subscriber.onError(new NullPointerException("The CompletionStage produced a null value"));
                        } else {
                            subscriber.onNext(value);
                            subscriber.onComplete();
                        }
                    }
                });
            }
        }

        @Override
        public void cancel() {
            cancelled.set(false);
            completableFuture.toCompletableFuture().cancel(false);
        }
    }
}
