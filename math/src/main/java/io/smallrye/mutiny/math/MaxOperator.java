package io.smallrye.mutiny.math;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Max operator emitting the max of the emitted items from upstream.
 * If the upstream emits a failure, the failure is propagated.
 * <p>
 * It only emits the maximum if the received item is larger than the previously emitted maxmimum.
 * <p>
 * Note that this operator relies on {@link Comparable}.
 *
 * @param <T> type of the incoming items.
 */
public class MaxOperator<T extends Comparable<T>> implements Function<Multi<T>, Multi<T>> {

    private final AtomicReference<T> max = new AtomicReference<>();

    @Override
    public Multi<T> apply(Multi<T> multi) {
        return multi
                .onItem().transformToMultiAndConcatenate(item -> {
                    if (max.compareAndSet(null, item)) {
                        return Multi.createFrom().item(item);
                    }

                    if (item == max.updateAndGet(t -> {
                        if (t.compareTo(item) < 0) {
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
