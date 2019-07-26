package io.smallrye.reactive.operators;


import io.smallrye.reactive.Uni;

import java.util.function.Consumer;

public class UniPeekOnEvent<T> extends UniOperator<T, T> {

    private final Consumer<? super T> onResult;
    private final Consumer<Throwable> onFailure;

    public UniPeekOnEvent(Uni<? extends T> upstream,
                          Consumer<? super T> onResult,
                          Consumer<Throwable> onFailure) {
        super(upstream);
        this.onResult = onResult;
        this.onFailure = onFailure;
    }

    @Override
    public void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onResult(T result) {
                if (invokeEventHandler(onResult, result, subscriber)) {
                    subscriber.onResult(result);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (invokeEventHandler(onFailure, failure, subscriber)) {
                    subscriber.onFailure(failure);
                }
            }
        });
    }

    private <E> boolean invokeEventHandler(Consumer<? super E> handler, E event,
                                           UniSerializedSubscriber<? super T> subscriber) {
        if (handler != null) {
            try {
                handler.accept(event);
            } catch (Throwable e) {
                subscriber.onFailure(e);
                return false;
            }
        }
        return true;
    }
}
