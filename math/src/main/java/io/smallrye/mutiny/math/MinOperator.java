package io.smallrye.mutiny.math;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Min operator emitting the min of the emitted items from upstream.
 * If the upstream emits a failure, the failure is propagated.
 *
 * It only emits the minimum if the received item is smaller than the previously emitted minimum.
 *
 * Note that this operator relies on {@link Comparable}.
 *
 * @param <T> type of the incoming items.
 */
public class MinOperator<T extends Comparable<T>> implements Function<Multi<T>, Multi<T>> {

    private final AtomicReference<T> min = new AtomicReference<>();

    @Override
    public Multi<T> apply(Multi<T> multi) {
        return multi
                .onItem().transformToMultiAndConcatenate(item -> {
                    if (min.compareAndSet(null, item)) {
                        return Multi.createFrom().item(item);
                    }

                    if (item == min.updateAndGet(t -> {
                        if (t.compareTo(item) > 0) {
                            return item;
                        }
                        return t;
                    })) {
                        return Multi.createFrom().item(item);
                    }

                    return Multi.createFrom().empty();
                }).skip().repetitions();
    }
}
