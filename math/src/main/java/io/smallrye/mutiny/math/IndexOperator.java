package io.smallrye.mutiny.math;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.tuples.Tuple2;

/**
 * Index operator emitting a {@link io.smallrye.mutiny.tuples.Tuple2 Tuple2&lt;Long, T&gt;}, with the index (0-based) of the
 * element and the element from upstream.
 *
 * If the upstream emits a failure, then, the failure is propagated.
 *
 * @param <T> type of the incoming items.
 */
public class IndexOperator<T>
        implements Function<Multi<T>, Multi<Tuple2<Long, T>>> {

    private final AtomicLong index = new AtomicLong();

    @Override
    public Multi<Tuple2<Long, T>> apply(Multi<T> multi) {
        return multi
                .onItem().transform(x -> Tuple2.of(index.getAndIncrement(), x));
    }
}
