package io.smallrye.mutiny.math;

import java.util.concurrent.atomic.AtomicReference;
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
public class AverageOperator<T extends Number> implements Function<Multi<T>, Multi<Double>> {

    private final AtomicReference<State> state = new AtomicReference<>();

    private static class State {
        double sum = 0.0d;
        long count = 0L;

        State(double sum, long count) {
            this.sum = sum;
            this.count = count;
        }
    }

    @Override
    public Multi<Double> apply(Multi<T> multi) {
        return multi
                .onItem().transform(x -> {
                    State newState = this.state.updateAndGet(s -> {
                        if (s == null) {
                            return new State(x.doubleValue(), 1L);
                        }
                        return new State(x.doubleValue() + s.sum, s.count + 1L);
                    });
                    return newState.sum / (double) newState.count;
                })
                .onCompletion().ifEmpty().continueWith(0.0d);
    }
}
