package io.smallrye.reactive.operators;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.propagateFailureEvent;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniCreateFromCompletionStage<O> extends UniOperator<Void, O> {
    private final Supplier<? extends CompletionStage<? extends O>> supplier;

    public UniCreateFromCompletionStage(Supplier<? extends CompletionStage<? extends O>> supplier) {
        super(null);
        this.supplier = nonNull(supplier, "supplier");
    }

    private static <O> void forwardFromCompletionStage(CompletionStage<? extends O> stage, UniSerializedSubscriber<? super O> subscriber) {
        subscriber.onSubscribe(() -> stage.toCompletableFuture().cancel(false));
        stage.whenComplete((res, fail) -> {
            if (fail != null) {
                subscriber.onFailure(fail);
            } else {
                subscriber.onItem(res);
            }
        });
    }

    @Override
    public void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        CompletionStage<? extends O> stage = supplier.get();

        if (stage == null) {
            propagateFailureEvent(subscriber, new NullPointerException("The produced completion stage is `null`"));
            return;
        }

        forwardFromCompletionStage(stage, subscriber);
    }
}
