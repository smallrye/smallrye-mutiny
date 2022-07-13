package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

/**
 * Multi operator re-subscribing to the upstream if it receives a failure event.
 * It can re-subscribe indefinitely (passing Long.MAX_VALUE as number of attempts) or a fixed number of times.
 *
 * @param <T> the type of item
 */
public final class MultiRetryOp<T> extends AbstractMultiOperator<T, T> {

    private final long times;
    private final Predicate<? super Throwable> onFailurePredicate;

    public MultiRetryOp(Multi<? extends T> upstream, Predicate<? super Throwable> onFailurePredicate, long times) {
        super(upstream);
        this.onFailurePredicate = onFailurePredicate;
        this.times = times;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        RetrySubscriber<T> subscriber = new RetrySubscriber<>(upstream, onFailurePredicate, downstream, times);

        downstream.onSubscribe(subscriber);

        if (!subscriber.isCancelled()) {
            subscriber.resubscribe();
        }
    }

    static final class RetrySubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Flow.Publisher<? extends T> upstream;
        private final AtomicInteger wip = new AtomicInteger();

        private long remaining;
        long produced;

        private final Predicate<? super Throwable> onFailurePredicate;

        RetrySubscriber(Flow.Publisher<? extends T> upstream, Predicate<? super Throwable> onFailurePredicate,
                MultiSubscriber<? super T> downstream, long attempts) {
            super(downstream);
            this.upstream = upstream;
            this.remaining = attempts;
            this.onFailurePredicate = onFailurePredicate;
        }

        @Override
        public void onItem(T t) {
            produced++;
            downstream.onItem(t);
        }

        @Override
        public void onFailure(Throwable t) {
            if (testOnFailurePredicate(t)) {
                return;
            }

            long r = remaining;
            if (r != Long.MAX_VALUE) {
                if (r == 0) {
                    // Forward
                    downstream.onFailure(t);
                    return;
                }
                remaining = r - 1;
            }
            resubscribe();
        }

        private boolean testOnFailurePredicate(Throwable t) {
            // The onFailurePredicate cannot be null.
            try {
                if (!onFailurePredicate.test(t)) {
                    cancel();
                    downstream.onFailure(t);
                    return true;
                }
            } catch (Throwable e) {
                cancel();
                downstream.onFailure(new CompositeException(e, t));
                return true;
            }
            return false;
        }

        void resubscribe() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (isCancelled()) {
                        return;
                    }
                    long c = produced;
                    if (c != 0L) {
                        produced = 0L;
                        emitted(c);
                    }
                    upstream.subscribe(Infrastructure.onMultiSubscription(upstream, this));
                } while (wip.decrementAndGet() != 0);
            }
        }
    }
}
