package io.smallrye.mutiny.operators.multi;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiIgnoreOp<T> extends AbstractMultiOperator<T, Void> {

    public MultiIgnoreOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super Void> downstream) {
        upstream.subscribe(new MultiIgnoreProcessor<>(downstream));
    }

    static class MultiIgnoreProcessor<T> extends MultiOperatorProcessor<T, Void> {
        MultiIgnoreProcessor(MultiSubscriber<? super Void> downstream) {
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
        public void onItem(T ignored) {
            // Ignoring
        }
    }
}
