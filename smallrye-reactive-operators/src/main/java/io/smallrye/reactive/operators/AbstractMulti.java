package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.MissingBackpressureException;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.groups.MultiOnEvent;
import io.smallrye.reactive.groups.MultiOnFailure;
import io.smallrye.reactive.groups.MultiOnResult;
import io.smallrye.reactive.groups.MultiSubscribe;
import io.smallrye.reactive.subscription.BackPressureFailure;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.CANCELLED;

public abstract class AbstractMulti<T> implements Multi<T> {


    protected abstract Flowable<T> flowable();

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Flowable<T> flowable = flowable();
        if (flowable == null) {
            throw new IllegalStateException("Invalid call to subscription, we don't have a stream");
        }
        //noinspection SubscriberImplementation
        flowable.subscribe(new Subscriber<T>() {

            AtomicReference<Subscription> reference = new AtomicReference<>();

            @Override
            public void onSubscribe(Subscription s) {
                if (reference.compareAndSet(null, s)) {
                    subscriber.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                            // pass through
                            s.request(n);
                        }

                        @Override
                        public void cancel() {
                            try {
                                s.cancel();
                            } finally {
                                reference.set(CANCELLED);
                            }
                        }
                    });
                } else {
                    s.cancel();
                }
            }

            @Override
            public void onNext(T result) {
                try {
                    subscriber.onNext(result);
                } catch (Exception e) {
                    Subscription subscription = reference.getAndSet(CANCELLED);
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }
            }

            @Override
            public void onError(Throwable failure) {
                try {
                    if (failure instanceof MissingBackpressureException) {
                        subscriber.onError(new BackPressureFailure(failure.getMessage()));
                    } else if (failure instanceof CompositeException) {
                        subscriber.onError(new io.smallrye.reactive.CompositeException(((CompositeException) failure).getExceptions()));
                    }else {
                        subscriber.onError(failure);
                    }
                } finally {
                    reference.set(CANCELLED);
                }
            }

            @Override
            public void onComplete() {
                try {
                    subscriber.onComplete();
                } finally {
                    reference.set(CANCELLED);
                }
            }
        });
    }

    @Override
    public MultiOnResult<T> onResult() {
        return new MultiOnResult<>(this);
    }

    @Override
    public MultiSubscribe<T> subscribe() {
        return new MultiSubscribe<>(this);
    }

    @Override
    public Uni<T> toUni() {
        return Uni.createFrom().publisher(this);
    }

    @Override
    public MultiOnFailure<T> onFailure() {
        return new MultiOnFailure<>(this, null);
    }

    @Override
    public MultiOnFailure<T> onFailure(Predicate<? super Throwable> predicate) {
        return new MultiOnFailure<>(this, predicate);
    }

    @Override
    public MultiOnFailure<T> onFailure(Class<? extends Throwable> typeOfFailure) {
        return new MultiOnFailure<>(this, typeOfFailure::isInstance);
    }

    @Override
    public MultiOnEvent<T> on() {
        return new MultiOnEvent<>(this);
    }
}
