package io.smallrye.reactive.operators;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniSubscription;
import io.smallrye.reactive.tuples.Functions;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniOnTermination<T> extends UniOperator<T, T> {
    private final Functions.TriConsumer<T, Throwable, Boolean> callback;

    public UniOnTermination(Uni<T> upstream, Functions.TriConsumer<T, Throwable, Boolean> callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        upstream().subscribe().withSubscriber(
                new UniDelegatingSubscriber<T, T>(subscriber) {
                    @Override
                    public void onSubscribe(UniSubscription subscription) {
                        super.onSubscribe(() -> {
                            subscription.cancel();
                            callback.accept(null, null, true);
                        });
                    }

                    @Override
                    public void onItem(T item) {
                        try {
                            callback.accept(item, null, false);
                        } catch (Exception e) {
                            subscriber.onFailure(e);
                            return;
                        }
                        subscriber.onItem(item);
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        try {
                            callback.accept(null, failure, false);
                        } catch (Exception e) {
                            subscriber.onFailure(new CompositeException(failure, e));
                            return;
                        }
                        subscriber.onFailure(failure);
                    }
                });
    }
}
