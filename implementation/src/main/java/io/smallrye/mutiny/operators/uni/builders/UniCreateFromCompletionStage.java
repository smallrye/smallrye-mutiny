package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.propagateFailureEvent;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCreateFromCompletionStage<T> extends AbstractUni<T> {
    private final Supplier<? extends CompletionStage<? extends T>> supplier;

    public UniCreateFromCompletionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        this.supplier = supplier; // Already checked
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

        new CompletionStageUniSubscription<T>(subscriber, stage).forward();
    }

    static class CompletionStageUniSubscription<T> implements UniSubscription {

        private final UniSubscriber<? super T> subscriber;
        private final CompletionStage<? extends T> stage;
        private volatile boolean cancelled = false;

        CompletionStageUniSubscription(UniSubscriber<? super T> subscriber, CompletionStage<? extends T> stage) {
            this.subscriber = subscriber;
            this.stage = stage;
        }

        public void forward() {
            subscriber.onSubscribe(this);
            stage.whenComplete(this::forwardResult);
        }

        private void forwardResult(T res, Throwable fail) {
            if (!cancelled) {
                if (fail != null) {
                    if (fail instanceof CompletionException) {
                        subscriber.onFailure(fail.getCause());
                    } else {
                        subscriber.onFailure(fail);
                    }
                } else {
                    subscriber.onItem(res);
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            stage.toCompletableFuture().cancel(false);
        }
    }
}
