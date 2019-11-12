package io.smallrye.reactive.operators.multi;

import static io.smallrye.reactive.helpers.Subscriptions.CANCELLED;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;

public final class MultiLastItemOp<T> extends AbstractMultiWithUpstream<T, T> {

    public MultiLastItemOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        upstream.subscribe(new MultiLastItemSubscriber<T>(downstream));
    }

    static final class MultiLastItemSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        T last;

        MultiLastItemSubscriber(Subscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T item) {
            last = item;
        }

        @Override
        public void onError(Throwable failure) {
            super.onError(failure);
            last = null;
        }

        @Override
        public void onComplete() {
            Subscription subscription = upstream.getAndSet(CANCELLED);
            if (subscription != CANCELLED) {
                T item = last;
                if (item != null) {
                    last = null; // release before calling the callback.
                    downstream.onNext(item);
                    downstream.onComplete();
                } else {
                    downstream.onComplete();
                }
            }
        }
    }
}
