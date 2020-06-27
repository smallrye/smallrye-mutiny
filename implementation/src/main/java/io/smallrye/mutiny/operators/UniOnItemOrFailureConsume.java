package io.smallrye.mutiny.operators;

import java.util.function.BiConsumer;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

public class UniOnItemOrFailureConsume<T> extends UniOperator<T, T> {

    private final BiConsumer<? super T, Throwable> callback;

    public UniOnItemOrFailureConsume(Uni<? extends T> upstream,
            BiConsumer<? super T, Throwable> callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onItem(T item) {
                if (invokeCallback(item, null, subscriber)) {
                    subscriber.onItem(item);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (invokeCallback(null, failure, subscriber)) {
                    subscriber.onFailure(failure);
                }
            }
        });
    }

    private boolean invokeCallback(T item, Throwable failure, UniSerializedSubscriber<? super T> subscriber) {
        try {
            callback.accept(item, failure);
            return true;
        } catch (Throwable e) {
            if (failure != null) {
                subscriber.onFailure(new CompositeException(failure, e));
            } else {
                subscriber.onFailure(e);
            }
            return false;
        }
    }
}
