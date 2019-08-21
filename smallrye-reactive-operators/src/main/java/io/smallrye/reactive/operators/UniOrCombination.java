package io.smallrye.reactive.operators;



import io.smallrye.reactive.Uni;
import io.smallrye.reactive.helpers.EmptyUniSubscription;
import io.smallrye.reactive.subscription.UniSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;


public class UniOrCombination<T> extends UniOperator<Void, T> {

    private final List<Uni<? super T>> challengers;

    public UniOrCombination(Iterable<? extends Uni<? super T>> iterable) {
        super(null);
        this.challengers = new ArrayList<>();
        nonNull(iterable, "iterable")
                .forEach(u -> challengers.add(nonNull(u, "iterable` must not contain a `null` value")));
    }

    public UniOrCombination(Uni<? super T>[] array) {
        super(null);
        nonNull(array, "array");
        this.challengers = new ArrayList<>();
        for (Uni<? super T> u : array) {
            challengers.add(nonNull(u, "array` must not contain a `null` value"));
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        if (challengers.isEmpty()) {
            subscriber.onSubscribe(EmptyUniSubscription.CANCELLED);
            subscriber.onItem(null);
            return;
        }

        if (challengers.size() == 1) {
            // Just subscribe to the first and unique uni.
            Uni<? super T> uni = challengers.get(0);
            uni.subscribe().withSubscriber((UniSubscriber) subscriber);
            return;
        }

        // Barrier - once set to {@code true} the signals are ignored
        AtomicBoolean completedOrCancelled = new AtomicBoolean();

        List<CompletableFuture<? super T>> futures = new ArrayList<>();
        challengers.forEach(uni -> {
            CompletableFuture<? super T> future = uni.subscribe().asCompletionStage();
            futures.add(future);
        });

        // Do not call unSubscribe until we get all the futures.
        // But at the same time we can't start resolving as we didn't give a subscription to the subscriber
        // yet.

        subscriber.onSubscribe(() -> {
            if (completedOrCancelled.compareAndSet(false, true)) {
                // Cancel all
                futures.forEach(cf -> cf.cancel(false));
            }
        });

        // Once the subscription has been given, start resolving
        futures.forEach(future ->
                future.whenComplete((res, fail) -> {
                    if (completedOrCancelled.compareAndSet(false, true)) {
                        // Cancel other
                        futures.forEach(cf -> {
                            if (cf != future) {
                                cf.cancel(false);
                            }
                        });
                        if (fail != null) {
                            subscriber.onFailure(fail);
                        } else {
                            subscriber.onItem((T) res);
                        }
                    }
                })
        );
    }
}
