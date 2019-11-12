
package io.smallrye.reactive.operators.multi;

import java.util.function.BiFunction;

import org.reactivestreams.Subscriber;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;

/**
 * Scan operator accumulating items of the same type as the upstream.
 * 
 * @param <T> the type of item
 */
public final class MultiScanOp<T> extends AbstractMultiWithUpstream<T, T> {

    private final BiFunction<T, ? super T, T> accumulator;

    public MultiScanOp(Multi<? extends T> upstream, BiFunction<T, ? super T, T> accumulator) {
        super(upstream);
        this.accumulator = ParameterValidation.nonNull(accumulator, "accumulator");
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        upstream.subscribe(new ScanSubscriber<>(downstream, accumulator));
    }

    static final class ScanSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final BiFunction<T, ? super T, T> accumulator;
        private T current;

        ScanSubscriber(Subscriber<? super T> downstream, BiFunction<T, ? super T, T> accumulator) {
            super(downstream);
            this.accumulator = accumulator;
        }

        @Override
        public void onNext(T item) {
            if (isDone()) {
                return;
            }

            T result = item;
            if (current != null) {
                try {
                    result = accumulator.apply(current, item);
                } catch (Throwable e) {
                    onError(e);
                    return;
                }
                if (result == null) {
                    onError(new NullPointerException(ParameterValidation.MAPPER_RETURNED_NULL));
                    return;
                }
            }

            current = result;
            downstream.onNext(item);

        }

        @Override
        public void onError(Throwable failure) {
            super.onError(failure);
            current = null;
        }

        @Override
        public void onComplete() {
            super.onComplete();
            current = null;
        }
    }
}
