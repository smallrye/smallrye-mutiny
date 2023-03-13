
package io.smallrye.mutiny.operators.multi;

import java.util.function.BiFunction;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Scan operator accumulating items of the same type as the upstream.
 *
 * @param <T> the type of item
 */
public final class MultiScanOp<T> extends AbstractMultiOperator<T, T> {

    private final BiFunction<T, ? super T, T> accumulator;

    public MultiScanOp(Multi<? extends T> upstream, BiFunction<T, ? super T, T> accumulator) {
        super(upstream);
        this.accumulator = ParameterValidation.nonNull(accumulator, "accumulator");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new ScanProcessor<>(downstream, accumulator));
    }

    static final class ScanProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final BiFunction<T, ? super T, T> accumulator;
        private T current;

        ScanProcessor(MultiSubscriber<? super T> downstream, BiFunction<T, ? super T, T> accumulator) {
            super(downstream);
            this.accumulator = accumulator;
        }

        @Override
        public void onItem(T item) {
            if (isDone()) {
                return;
            }

            T result = item;
            if (current != null) {
                try {
                    result = accumulator.apply(current, item);
                } catch (Throwable e) {
                    onFailure(e);
                    return;
                }
                if (result == null) {
                    onFailure(new NullPointerException(ParameterValidation.MAPPER_RETURNED_NULL));
                    return;
                }
            }

            current = result;
            downstream.onItem(result);

        }

        @Override
        public void onFailure(Throwable failure) {
            super.onFailure(failure);
            current = null;
        }

        @Override
        public void onCompletion() {
            super.onCompletion();
            current = null;
        }
    }
}
