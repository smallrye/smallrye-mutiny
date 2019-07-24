package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.groups.MultiOnResult;
import io.smallrye.reactive.groups.MultiSubscribe;
import io.smallrye.reactive.subscription.BackPressureFailure;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class AbstractMulti<T> implements Multi<T> {


    protected abstract Flowable<T> flowable();

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Flowable<T> flowable = flowable();
        if (flowable == null) {
            throw new IllegalStateException("Invalid call to subscription, we don't have a stream");
        }
        flowable.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriber.onSubscribe(s);
            }

            @Override
            public void onNext(T result) {
                subscriber.onNext(result);
            }

            @Override
            public void onError(Throwable failure) {
                if (failure instanceof MissingBackpressureException) {
                    subscriber.onError(new BackPressureFailure(failure.getMessage()));
                } else {
                    subscriber.onError(failure);
                }
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
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
}
