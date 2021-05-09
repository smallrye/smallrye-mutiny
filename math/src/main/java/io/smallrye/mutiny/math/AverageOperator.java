package io.smallrye.mutiny.math;

import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Average operator emitting the average of the items emitted by the upstream.
 * <p>
 * Everytime it gets an item from upstream, it emits the <em>average</em> of the already received items.
 * If the stream emits the completion event without having emitting any item before, 0 is emitted, followed by the
 * completion event.
 * If the upstream emits a failure, then, the failure is propagated.
 */
public class AverageOperator<T extends Number>
        implements Function<Multi<T>, Multi<Double>> {

    private double sum = 0.0d;
    private long count = 0L;

    @Override
    public Multi<Double> apply(Multi<T> multi) {
        return multi
                .onItem().transform(x -> {
                    count = count + 1L;
                    sum = sum + x.doubleValue();
                    return sum / count;
                })
                .onCompletion().ifEmpty().continueWith(0.0d);
    }
}
