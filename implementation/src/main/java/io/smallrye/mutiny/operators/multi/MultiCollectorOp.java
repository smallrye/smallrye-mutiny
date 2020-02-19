package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public final class MultiCollectorOp<T, A, R> extends AbstractMultiOperator<T, R> {

    private final Collector<? super T, A, ? extends R> collector;
    private final boolean acceptNullAsInitialValue;

    public MultiCollectorOp(Multi<T> upstream, Collector<? super T, A, ? extends R> collector,
            boolean acceptNullAsInitialValue) {
        super(upstream);
        this.collector = collector;
        this.acceptNullAsInitialValue = acceptNullAsInitialValue;
    }

    @Override
    public void subscribe(MultiSubscriber<? super R> downstream) {
        A initialValue;
        BiConsumer<A, ? super T> accumulator;
        Function<A, ? extends R> finisher;

        try {
            initialValue = collector.supplier().get();
            accumulator = collector.accumulator();
            finisher = collector.finisher();
        } catch (Throwable ex) {
            Subscriptions.fail(downstream, ex, upstream);
            return;
        }

        if (initialValue == null && !acceptNullAsInitialValue) {
            Subscriptions.fail(downstream, new NullPointerException(SUPPLIER_PRODUCED_NULL), upstream);
            return;
        }

        if (accumulator == null) {
            Subscriptions.fail(downstream, new NullPointerException("`accumulator` must not be `null`"), upstream);
            return;
        }

        CollectorProcessor<? super T, A, ? extends R> processor = new CollectorProcessor<>(downstream, initialValue,
                accumulator, finisher);
        upstream.subscribe().withSubscriber(processor);
    }

    static class CollectorProcessor<T, A, R> extends MultiOperatorProcessor<T, R> {

        private final BiConsumer<A, T> accumulator;
        private final Function<A, R> finisher;
        // Only accessed in the serialized callbacks
        private A intermediate;

        CollectorProcessor(MultiSubscriber<? super R> downstream,
                A initialValue, BiConsumer<A, T> accumulator, Function<A, R> finisher) {
            super(downstream);
            this.intermediate = initialValue;
            this.accumulator = accumulator;
            this.finisher = finisher;
        }

        @Override
        public void onItem(T item) {
            if (upstream.get() != CANCELLED) {
                try {
                    accumulator.accept(intermediate, item);
                } catch (Throwable ex) {
                    failAndCancel(ex);
                }
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = upstream.getAndSet(Subscriptions.CANCELLED);
            if (subscription != Subscriptions.CANCELLED) {
                R result;

                try {
                    result = finisher.apply(intermediate);
                } catch (Throwable ex) {
                    downstream.onFailure(ex);
                    return;
                }

                intermediate = null;
                downstream.onItem(result);
                downstream.onCompletion();
            }
        }

        @Override
        public void request(long n) {
            // The subscriber may request only 1 but as we don't know how much we get, we request MAX.
            // This could be changed with call to request in the OnNext/OnItem
            super.request(Long.MAX_VALUE);
        }

    }
}
