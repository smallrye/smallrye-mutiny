package io.smallrye.mutiny.math;

import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Max operator emitting the max of the emitted items from upstream.
 * If the upstream emits a failure, the failure is propagated.
 *
 * It only emits the maximum if the received item is larger than the previously emitted maxmimum.
 *
 * Note that this operator relies on {@link Comparable}.
 *
 * @param <T> type of the incoming items.
 */
public class MaxOperator<T extends Comparable<T>>
        implements Function<Multi<T>, Multi<T>> {

    private T max = null;

    @Override
    public Multi<T> apply(Multi<T> multi) {
        return multi
                .onItem().transformToMultiAndConcatenate(item -> {
                    if (max == null || max.compareTo(item) < 0) {
                        max = item;
                        return Multi.createFrom().item(max);
                    }
                    return Multi.createFrom().empty();
                });
    }
}
