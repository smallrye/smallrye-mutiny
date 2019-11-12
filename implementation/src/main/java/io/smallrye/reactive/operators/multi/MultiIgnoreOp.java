package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.Multi;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MultiIgnoreOp<T> extends AbstractMultiWithUpstream<T, Void> {

    public MultiIgnoreOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(Subscriber<? super Void> downstream) {
        upstream.subscribe(new MultiIgnoreSubscriber<>(downstream));
    }

    static class MultiIgnoreSubscriber<T> extends MultiOperatorSubscriber<T, Void> {
        MultiIgnoreSubscriber(Subscriber<? super Void> downstream) {
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
