package io.smallrye.reactive.operators.multi;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.Subscriptions;

public final class MultiCollectorOp<T, A, R> extends AbstractMultiWithUpstream<T, R> {

    private final Collector<? super T, A, ? extends R> collector;

    public MultiCollectorOp(Multi<T> upstream, Collector<? super T, A, ? extends R> collector) {
        super(upstream);
        this.collector = collector;
    }

    @Override
    public void subscribe(Subscriber<? super R> downstream) {
        A initialValue;
        BiConsumer<A, ? super T> accumulator;
        Function<A, ? extends R> finisher;

        try {
            initialValue = collector.supplier().get();
            accumulator = collector.accumulator();
            finisher = collector.finisher();
        } catch (Exception ex) {
            Subscriptions.fail(downstream, ex);
            return;
        }

        if (initialValue == null) {
            Subscriptions.fail(downstream, new NullPointerException(SUPPLIER_PRODUCED_NULL));
            return;
        }

        if (accumulator == null) {
            Subscriptions.fail(downstream, new NullPointerException("`accumulator` must not be `null`"));
            return;
        }

        upstream.subscribe(new CollectorSubscriber<>(downstream, initialValue, accumulator, finisher));
    }

    static class CollectorSubscriber<T, A, R> extends MultiOperatorSubscriber<T, R> {

        private final BiConsumer<A, T> accumulator;
        private final Function<A, R> finisher;
        // Only accessed in the serialized callbacks
        private A intermediate;

        CollectorSubscriber(Subscriber<? super R> downstream,
                A initialValue, BiConsumer<A, T> accumulator, Function<A, R> finisher) {
            super(downstream);
            this.intermediate = initialValue;
            this.accumulator = accumulator;
            this.finisher = finisher;
        }

        @Override
        public void onNext(T item) {
            if (upstream.get() != CANCELLED) {
                try {
                    accumulator.accept(intermediate, item);
                } catch (Exception ex) {
                    onError(ex);
                }
            }
        }

        @Override
        public void onComplete() {
            Subscription subscription = upstream.getAndSet(Subscriptions.CANCELLED);
            if (subscription != Subscriptions.CANCELLED) {
                R result;

                try {
                    result = finisher.apply(intermediate);
                } catch (Exception ex) {
                    onError(ex);
                    return;
                }

                intermediate = null;
                downstream.onNext(result);
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            // The subscriber may request only 1 but as we don't know how much we get, we request MAX.
            // This could we changed with call to request in the OnNext
            super.request(Long.MAX_VALUE);
        }
    }
}
