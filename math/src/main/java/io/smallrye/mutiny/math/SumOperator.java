package io.smallrye.mutiny.math;

import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Sum operator emitting the current sum of the item emitted by the upstream.
 * Everytime it gets an item from upstream, it emits the <em>sum</em> of the previous elements
 * If the stream emits the completion event without having emitting any item before, 0 is emitted, followed by the
 * completion event.
 * If the upstream emits a failure, the failure is propagated.
 *
 * @param <T> type of the incoming items, must be a {@link Number}.
 */
public class SumOperator<T extends Number>
        implements Function<Multi<T>, Multi<Double>> {

    private double sum = 0.0d;

    @Override
    public Multi<Double> apply(Multi<T> multi) {
        return multi
                .onItem().transform(x -> {
                    sum = sum + x.doubleValue();
                    return sum;
                })
                .onCompletion().ifEmpty().continueWith(0.0d);
    }
}
