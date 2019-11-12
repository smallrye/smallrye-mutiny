package io.smallrye.reactive.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.subscription.SwitchableSubscriptionSubscriber;

/**
 * Multi operator re-subscribing to the upstream if if receives a failure event.
 * It can re-subscribe indefinitely (passing Long.MAX_VALUE as number of attempts) or a fixed number of times.
 *
 * @param <T> the type of item
 */
public final class MultiRetryOp<T> extends AbstractMultiWithUpstream<T, T> {

    private final long times;

    public MultiRetryOp(Multi<? extends T> upstream, long times) {
        super(upstream);
        this.times = ParameterValidation.positive(times, "times");
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        RetrySubscriber<T> subscriber = new RetrySubscriber<>(upstream, downstream, times);

        downstream.onSubscribe(subscriber);

        if (!subscriber.isCancelled()) {
            subscriber.resubscribe();
        }
    }

    static final class RetrySubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Publisher<? extends T> upstream;
        private final AtomicInteger wip = new AtomicInteger();

        private long remaining;
        long produced;

        RetrySubscriber(Publisher<? extends T> upstream, Subscriber<? super T> downstream, long attempts) {
            super(downstream);
            this.upstream = upstream;
            this.remaining = attempts;
        }

        @Override
        public void onNext(T t) {
            produced++;
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                if (r == 0) {
                    // Forward
                    downstream.onError(t);
                    return;
                }
                remaining = r - 1;
            }
            resubscribe();
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
                    upstream.subscribe(this);
                } while (wip.decrementAndGet() != 0);
            }
        }
    }
}
