package io.smallrye.reactive.operators.multi;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;

public class MultiIgnoreOp<T> extends AbstractMultiOperator<T, Void> {

    public MultiIgnoreOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(Subscriber<? super Void> downstream) {
        upstream.subscribe(new MultiIgnoreProcessor<>(downstream));
    }

    static class MultiIgnoreProcessor<T> extends MultiOperatorProcessor<T, Void> {
        MultiIgnoreProcessor(Subscriber<? super Void> downstream) {
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
        public void onNext(T ignored) {
            // Ignoring
        }
    }
}
