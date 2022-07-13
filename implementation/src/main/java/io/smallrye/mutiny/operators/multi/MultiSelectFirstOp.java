package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Takes the n first items emitted by the upstream, cancelling the subscription after that.
 * <p>
 * If n == 0, the subscriber gets completed if the upstream emits the completion, a failure signal, or a first (dropped)
 * item.
 *
 * @param <T> the type of item
 */
public final class MultiSelectFirstOp<T> extends AbstractMultiOperator<T, T> {

    private final long numberOfItems;

    public MultiSelectFirstOp(Multi<? extends T> upstream, long numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "subscriber");
        upstream.subscribe(new MultiSelectFirstProcessor<>(downstream, numberOfItems));
    }

    static final class MultiSelectFirstProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final long numberOfItems;
        private long remaining;
        private final AtomicInteger wip = new AtomicInteger();

        MultiSelectFirstProcessor(MultiSubscriber<? super T> downstream, long numberOfItems) {
            super(downstream);
            this.numberOfItems = numberOfItems;
            this.remaining = numberOfItems;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (compareAndSetUpstreamSubscription(null, s)) {
                if (numberOfItems == 0) {
                    getAndSetUpstreamSubscription(CANCELLED).cancel();
                    Subscriptions.complete(downstream);
                } else {
                    downstream.onSubscribe(this);
                }
            } else {
                s.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            if (getUpstreamSubscription() == Subscriptions.CANCELLED) {
                return;
            }

            MultiSubscriber<? super T> actual = downstream;

            long r = remaining;

            if (r == 0) {
                getAndSetUpstreamSubscription(CANCELLED).cancel();
                actual.onCompletion();
                return;
            }

            remaining = --r;
            downstream.onItem(t);
            if (r == 0L) {
                getAndSetUpstreamSubscription(CANCELLED).cancel();
                actual.onCompletion();
            }
        }

        @Override
        public void request(long n) {
            Subscription actual = getUpstreamSubscription();
            if (wip.compareAndSet(0, 1)) {
                if (n >= this.numberOfItems) {
                    actual.request(Long.MAX_VALUE);
                } else {
                    actual.request(n);
                }
                return;
            }
            actual.request(n);
        }
    }

}
