package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.Flow.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public final class MultiLastItemOp<T> extends AbstractMultiOperator<T, T> {

    public MultiLastItemOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiLastItemProcessor<T>(downstream));
    }

    static final class MultiLastItemProcessor<T> extends MultiOperatorProcessor<T, T> {

        T last;

        MultiLastItemProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void request(long numberOfItems) {
            // Ignored, we already requested Long.MAX
        }

        @Override
        public void onItem(T item) {
            last = item;
        }

        @Override
        public void onFailure(Throwable failure) {
            super.onFailure(failure);
            last = null;
        }

        @Override
        public void onCompletion() {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                T item = last;
                MultiSubscriber<? super T> subscriber = downstream;
                if (item != null) {
                    last = null; // release before calling the callback.
                    subscriber.onItem(item);
                }
                subscriber.onCompletion();
            }
        }
    }
}
