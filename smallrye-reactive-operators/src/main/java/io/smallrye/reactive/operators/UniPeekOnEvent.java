package io.smallrye.reactive.operators;


import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.propagateFailureEvent;

public class UniPeekOnEvent<T> extends UniOperator<T, T> {


    private final Consumer<? super UniSubscription> onSubscription;
    private final Consumer<? super T> onResult;
    private final Consumer<Throwable> onFailure;
    private final Runnable onCancellation;
    private final BiConsumer<? super T, Throwable> onTerminate;

    public UniPeekOnEvent(Uni<? extends T> upstream,
                          Consumer<? super UniSubscription> onSubscription,
                          Consumer<? super T> onResult,
                          Consumer<Throwable> onFailure,
                          Runnable onCancellation,
                          BiConsumer<? super T, Throwable> onTerminate) {
        super(upstream);
        this.onSubscription = onSubscription;
        this.onResult = onResult;
        this.onFailure = onFailure;
        this.onCancellation = onCancellation;
        this.onTerminate = onTerminate;
    }

    @Override
    public void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        upstream().subscribe().withSubscriber(new UniSubscriber<T>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                UniSubscription sub = () -> {
                    subscription.cancel();
                    if (onCancellation != null) {
                        onCancellation.run();
                    }
                };

                if (onSubscription != null) {
                    try {
                        onSubscription.accept(subscription);
                    } catch (Throwable e) {
                        propagateFailureEvent(subscriber, e);
                        return;
                    }
                }

                subscriber.onSubscribe(sub);
            }

            @Override
            public void onResult(T result) {
                if (invokeEventHandler(onResult, result, subscriber)) {
                    subscriber.onResult(result);
                    callOnTerminate(result, null);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (invokeEventHandler(onFailure, failure, subscriber)) {
                    subscriber.onFailure(failure);
                    callOnTerminate(null, failure);
                }
            }
        });
    }

    private void callOnTerminate(T result, Throwable failure) {
        if (onTerminate != null) {
            onTerminate.accept(result, failure);
        }
    }

    private <E> boolean invokeEventHandler(Consumer<? super E> handler, E event, UniSerializedSubscriber<? super T> subscriber) {
        if (handler != null) {
            try {
                handler.accept(event);
            } catch (Throwable e) {
                subscriber.onFailure(e);
                callOnTerminate(null, e);
                return false;
            }
        }
        return true;
    }
}
