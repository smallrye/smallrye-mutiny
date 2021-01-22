package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.propagateFailureEvent;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniCreateFromCompletionStage<T> extends AbstractUni<T> {
    private final Supplier<? extends CompletionStage<? extends T>> supplier;

    public UniCreateFromCompletionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        this.supplier = supplier; // Already checked
    }

    static <O> void forwardFromCompletionStage(CompletionStage<? extends O> stage,
            UniSubscriber<? super O> subscriber) {
        subscriber.onSubscribe(() -> stage.toCompletableFuture().cancel(false));
        stage.whenComplete((res, fail) -> {
            if (fail != null) {
                if (fail instanceof CompletionException) {
                    subscriber.onFailure(fail.getCause());
                } else {
                    subscriber.onFailure(fail);
                }
            } else {
                subscriber.onItem(res);
            }
        });
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        CompletionStage<? extends T> stage;
        try {
            stage = supplier.get();
        } catch (Throwable e) {
            propagateFailureEvent(subscriber, e);
            return;
        }

        if (stage == null) {
            propagateFailureEvent(subscriber, new NullPointerException("The produced CompletionStage is `null`"));
            return;
        }

        forwardFromCompletionStage(stage, subscriber);
    }
}
