package io.smallrye.reactive.operators;

import java.util.function.Consumer;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Uni;

public class UniOnEventConsume<T> extends UniOperator<T, T> {

    private final Consumer<? super T> onResult;
    private final Consumer<Throwable> onFailure;

    public UniOnEventConsume(Uni<? extends T> upstream,
            Consumer<? super T> onResult,
            Consumer<Throwable> onFailure) {
        super(upstream);
        this.onResult = onResult;
        this.onFailure = onFailure;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onItem(T item) {
                if (invokeEventHandler(onResult, item, false, subscriber)) {
                    subscriber.onItem(item);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (invokeEventHandler(onFailure, failure, true, subscriber)) {
                    subscriber.onFailure(failure);
                }
            }
        });
    }

    private <E> boolean invokeEventHandler(Consumer<? super E> handler, E event, boolean wasCalledByOnFailure,
            UniSerializedSubscriber<? super T> subscriber) {
        if (handler != null) {
            try {
                handler.accept(event);
            } catch (Throwable e) {
                if (wasCalledByOnFailure) {
                    subscriber.onFailure(new CompositeException((Throwable) event, e));
                } else {
                    subscriber.onFailure(e);
                }
                return false;
            }
        }
        return true;
    }
}
