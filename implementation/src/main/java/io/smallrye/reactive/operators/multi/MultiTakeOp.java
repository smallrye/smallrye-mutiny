package io.smallrye.reactive.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;

/**
 * Takes the n first items emitted by the upstream, cancelling the subscription after that.
 * <p>
 * If n == 0, the subscriber gets completed if the upstream emits the completion, a failure signal, or a first (dropped)
 * item.
 *
 * @param <T> the type of item
 */
public final class MultiTakeOp<T> extends AbstractMultiWithUpstream<T, T> {

    private final long numberOfItems;

    public MultiTakeOp(Multi<? extends T> upstream, long numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        upstream.subscribe(new TakeSubscriber<>(downstream, numberOfItems));
    }

    static final class TakeSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final long numberOfItems;
        private long remaining;
        private AtomicInteger wip = new AtomicInteger();

        TakeSubscriber(Subscriber<? super T> downstream, long numberOfItems) {
            super(downstream);
            this.numberOfItems = numberOfItems;
            this.remaining = numberOfItems;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (upstream.compareAndSet(null, s)) {
                if (numberOfItems == 0) {
                    upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                    Subscriptions.complete(downstream);
                } else {
                    downstream.onSubscribe(this);
                }
            } else {
                s.cancel();
            }
        }

        @Override
        public void onNext(T t) {
            if (upstream.get() == Subscriptions.CANCELLED) {
                return;
            }

            long r = remaining;

            if (r == 0) {
                upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                downstream.onComplete();
                return;
            }

            remaining = --r;
            downstream.onNext(t);
            if (r == 0L) {
                upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            Subscription actual = upstream.get();
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
