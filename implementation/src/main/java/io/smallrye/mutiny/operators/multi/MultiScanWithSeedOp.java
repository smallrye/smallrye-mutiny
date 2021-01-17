
package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

public final class MultiScanWithSeedOp<T, R> extends AbstractMultiOperator<T, R> {

    private final BiFunction<R, ? super T, R> accumulator;

    private final Supplier<R> seed;

    public MultiScanWithSeedOp(Multi<? extends T> upstream, Supplier<R> seed, BiFunction<R, ? super T, R> accumulator) {
        super(upstream);
        this.seed = seed;
        this.accumulator = accumulator;
    }

    @Override
    public void subscribe(MultiSubscriber<? super R> downstream) {
        ScanSubscriber<T, R> subscriber = new ScanSubscriber<>(upstream, downstream, accumulator, seed);

        downstream.onSubscribe(subscriber);

        if (!subscriber.isCancelled()) {
            subscriber.onCompletion();
        }
    }

    static final class ScanSubscriber<T, R> extends SwitchableSubscriptionSubscriber<R> {

        private final Multi<? extends T> upstream;
        private final Supplier<R> initialSupplier;
        private final BiFunction<R, ? super T, R> accumulator;
        private final AtomicInteger wip = new AtomicInteger();
        long produced;

        private ScanSeedProcessor<T, R> subscriber;

        ScanSubscriber(Multi<? extends T> upstream,
                MultiSubscriber<? super R> downstream,
                BiFunction<R, ? super T, R> accumulator,
                Supplier<R> seed) {
            super(downstream);
            this.upstream = upstream;
            this.accumulator = accumulator;
            this.initialSupplier = seed;
        }

        @Override
        public void onCompletion() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (isCancelled()) {
                        return;
                    }

                    if (subscriber != null && currentUpstream.get() == subscriber) {
                        downstream.onCompletion();
                        return;
                    }

                    long p = produced;
                    if (p != 0L) {
                        produced = 0L;
                        emitted(p);
                    }

                    if (subscriber == null) {
                        R initialValue;

                        try {
                            initialValue = initialSupplier.get();
                        } catch (Throwable e) {
                            onFailure(e);
                            return;
                        }

                        if (initialValue == null) {
                            onFailure(new NullPointerException("The seed cannot be `null`"));
                            return;
                        }
                        // Switch.
                        onSubscribe(Subscriptions.single(this, initialValue));
                        subscriber = new ScanSeedProcessor<>(this, accumulator, initialValue);
                    } else {
                        upstream.subscribe(Infrastructure.onMultiSubscription(upstream, subscriber));
                    }

                    if (isCancelled()) {
                        return;
                    }
                } while (wip.decrementAndGet() != 0);
            }

        }

        @Override
        public void onItem(R r) {
            produced++;
            downstream.onItem(r);
        }
    }

    private static final class ScanSeedProcessor<T, R> extends MultiOperatorProcessor<T, R> {

        private final BiFunction<R, ? super T, R> accumulator;
        R current;

        ScanSeedProcessor(MultiSubscriber<? super R> downstream,
                BiFunction<R, ? super T, R> accumulator,
                R initial) {
            super(downstream);
            this.accumulator = accumulator;
            this.current = initial;
        }

        @Override
        public void onCompletion() {
            super.onCompletion();
            current = null;
        }

        @Override
        public void onFailure(Throwable failure) {
            super.onFailure(failure);
            current = null;
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            R r = current;
            try {
                r = accumulator.apply(r, t);
            } catch (Throwable e) {
                onFailure(e);
                return;
            }
            if (r == null) {
                onFailure(new NullPointerException("The accumulator returned a null value"));
                return;
            }
            downstream.onItem(r);
            current = r;
        }
    }
}
