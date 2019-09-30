package io.smallrye.reactive.unimulti.operators;

import io.smallrye.reactive.unimulti.helpers.ParameterValidation;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static io.smallrye.reactive.unimulti.helpers.EmptyUniSubscription.propagateFailureEvent;

public class UniCreateFromCompletionStage<O> extends UniOperator<Void, O> {
    private final Supplier<? extends CompletionStage<? extends O>> supplier;

    public UniCreateFromCompletionStage(Supplier<? extends CompletionStage<? extends O>> supplier) {
        super(null);
        this.supplier = ParameterValidation.nonNull(supplier, "supplier");
    }

    private static <O> void forwardFromCompletionStage(CompletionStage<? extends O> stage,
            UniSerializedSubscriber<? super O> subscriber) {
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
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        CompletionStage<? extends O> stage = supplier.get();

        if (stage == null) {
            propagateFailureEvent(subscriber, new NullPointerException("The produced completion stage is `null`"));
            return;
        }

        forwardFromCompletionStage(stage, subscriber);
    }
}
