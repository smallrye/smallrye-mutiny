package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.Subscriptions.getInvalidRequestException;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiBroadcast;
import io.smallrye.mutiny.groups.MultiCollect;
import io.smallrye.mutiny.groups.MultiConvert;
import io.smallrye.mutiny.groups.MultiGroup;
import io.smallrye.mutiny.groups.MultiOnCompletion;
import io.smallrye.mutiny.groups.MultiOnEvent;
import io.smallrye.mutiny.groups.MultiOnFailure;
import io.smallrye.mutiny.groups.MultiOnItem;
import io.smallrye.mutiny.groups.MultiOverflow;
import io.smallrye.mutiny.groups.MultiSubscribe;
import io.smallrye.mutiny.groups.MultiTransform;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiCacheOp;
import io.smallrye.mutiny.operators.multi.MultiEmitOnOp;
import io.smallrye.mutiny.operators.multi.MultiSubscribeOnOp;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SerializedSubscriber;

public abstract class AbstractMulti<T> implements Multi<T> {

    public void subscribe(MultiSubscriber<? super T> subscriber) {
        this.subscribe(Infrastructure.onMultiSubscription(this, subscriber));
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            // NOTE The Reactive Streams TCK mandates throwing an NPE.
            throw new NullPointerException("Subscriber is `null`");
        }

        //noinspection SubscriberImplementation
        this.subscribe(new SerializedSubscriber<>(new Subscriber<T>() {

            AtomicReference<Subscription> reference = new AtomicReference<>();

            @Override
            public void onSubscribe(Subscription s) {
                if (reference.compareAndSet(null, s)) {
                    subscriber.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                            // validate and pass through
                            if (n <= 0) {
                                onError(getInvalidRequestException());
                                return;
                            }
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
            public void onNext(T item) {
                if (item == null) {
                    Subscription subscription = reference.getAndSet(CANCELLED);
                    if (subscription != null) {
                        subscription.cancel();
                    }
                    throw new NullPointerException("`null` is not a valid item");
                }
                try {
                    subscriber.onNext(item);
                } catch (Throwable e) {
                    Subscription subscription = reference.getAndSet(CANCELLED);
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }
            }

            @Override
            public void onError(Throwable failure) {
                if (failure == null) {
                    throw new NullPointerException("The failure must not be `null`");
                }
                try {
                    subscriber.onError(failure);
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
        }));
    }

    @Override
    public MultiOnItem<T> onItem() {
        return new MultiOnItem<>(this);
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

    @Override
    public Multi<T> cache() {
        return Infrastructure.onMultiCreation(new MultiCacheOp<>(this));
    }

    @Override
    public MultiCollect<T> collectItems() {
        return new MultiCollect<>(this);
    }

    @Override
    public MultiGroup<T> groupItems() {
        return new MultiGroup<>(this);
    }

    @Override
    public Multi<T> emitOn(Executor executor) {
        return Infrastructure.onMultiCreation(new MultiEmitOnOp<>(this, nonNull(executor, "executor")));
    }

    @Override
    public Multi<T> runSubscriptionOn(Executor executor) {
        return Infrastructure.onMultiCreation(new MultiSubscribeOnOp<>(this, executor));
    }

    @Override
    public MultiOnCompletion<T> onCompletion() {
        return new MultiOnCompletion<>(this);
    }

    @Override
    public MultiTransform<T> transform() {
        return new MultiTransform<>(this);
    }

    @Override
    public MultiOverflow<T> onOverflow() {
        return new MultiOverflow<>(this);
    }

    @Override
    public MultiBroadcast<T> broadcast() {
        return new MultiBroadcast<>(this);
    }

    @Override
    public MultiConvert<T> convert() {
        return new MultiConvert<>(this);
    }

}
