
package io.smallrye.reactive.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.reactivestreams.Subscriber;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.subscription.SwitchableSubscriptionSubscriber;

public final class MultiScanWithSeedOp<T, R> extends AbstractMultiOperator<T, R> {

    private final BiFunction<R, ? super T, R> accumulator;

    private final Supplier<R> seed;

    public MultiScanWithSeedOp(Multi<? extends T> upstream, Supplier<R> seed, BiFunction<R, ? super T, R> accumulator) {
        super(upstream);
        this.seed = ParameterValidation.nonNull(seed, "seed");
        this.accumulator = ParameterValidation.nonNull(accumulator, "accumulator");
    }

    @Override
    public void subscribe(Subscriber<? super R> downstream) {
        ScanSubscriber<T, R> subscriber = new ScanSubscriber<>(upstream, downstream, accumulator, seed);

        downstream.onSubscribe(subscriber);

        if (!subscriber.isCancelled()) {
            subscriber.onComplete();
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
                Subscriber<? super R> downstream,
                BiFunction<R, ? super T, R> accumulator,
                Supplier<R> seed) {
            super(downstream);
            this.upstream = upstream;
            this.accumulator = accumulator;
            this.initialSupplier = seed;
        }

        @Override
        public void onComplete() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (isCancelled()) {
                        return;
                    }

                    if (subscriber != null && currentUpstream.get() == subscriber) {
                        downstream.onComplete();
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
                            onError(e);
                            return;
                        }

                        if (initialValue == null) {
                            onError(new NullPointerException("The seed cannot be `null`"));
                            return;
                        }
                        // Switch.
                        onSubscribe(Subscriptions.single(this, initialValue));
                        subscriber = new ScanSeedProcessor<>(this, accumulator, initialValue);
                    } else {
                        upstream.subscribe(subscriber);
                    }

                    if (isCancelled()) {
                        return;
                    }
                } while (wip.decrementAndGet() != 0);
            }

        }

        @Override
        public void onNext(R r) {
            produced++;
            downstream.onNext(r);
        }
    }

    private static final class ScanSeedProcessor<T, R> extends MultiOperatorProcessor<T, R> {

        private final BiFunction<R, ? super T, R> accumulator;
        R current;

        ScanSeedProcessor(Subscriber<? super R> downstream,
                BiFunction<R, ? super T, R> accumulator,
                R initial) {
            super(downstream);
            this.accumulator = accumulator;
            this.current = initial;
        }

        @Override
        public void onComplete() {
            super.onComplete();
            current = null;
        }

        @Override
        public void onError(Throwable failure) {
            super.onError(failure);
            current = null;
        }

        @Override
        public void onNext(T t) {
            if (isDone()) {
                return;
            }

            R r = current;
            try {
                r = accumulator.apply(r, t);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            if (r == null) {
                onError(new NullPointerException("The accumulator returned a null value"));
                return;
            }
            downstream.onNext(r);
            current = r;
        }
    }
}
