package io.smallrye.mutiny.operators.multi;

import java.util.Objects;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiIgnoreOp<T> extends AbstractMultiOperator<T, Void> {

    public MultiIgnoreOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super Void> downstream) {
        upstream.subscribe().withSubscriber(new MultiIgnoreProcessor<>(Objects.requireNonNull(downstream)));
    }

    public static class MultiIgnoreProcessor<T> extends MultiOperatorProcessor<T, Void> {
        public MultiIgnoreProcessor(MultiSubscriber<? super Void> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                // Propagate subscription to downstream.
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void request(long numberOfItems) {
            // Request is handled by the onSubscribe method
            // Just validate the parameter for compliance with Reactive Streams.
            if (numberOfItems <= 0) {
                onFailure(new IllegalArgumentException("Invalid number of request, must be greater than 0"));
            }
        }

        @Override
        public void onItem(T ignored) {
            // Ignoring
        }
    }
}
