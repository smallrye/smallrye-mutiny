package io.smallrye.reactive.unimulti.groups;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.*;

import java.time.Duration;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.operators.MultiCollector;

public class MultiGroupIntoMultis<T> {

    private final Multi<T> upstream;

    public MultiGroupIntoMultis(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Creates a {@link Multi} that emits {@link Multi} of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits connected, non-overlapping stream of items, each of a fixed duration specified
     * by the {@code duration} parameter. If, during the configured time window, no items are emitted by the upstream
     * {@link Multi}, an empty {@code Multi} is emitted by the returned {@link Multi}.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the resulting {@link Multi} emits the current
     * {@link Multi} and propagates the completion event.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param duration the period of time each Multi collects items before it is emitted and replaced with a new
     *        Multi. Must be non {@code null} and positive.
     * @return a Multi that emits every {@code duration} with the items emitted by the upstream multi during the time
     *         window.
     */
    public Multi<Multi<T>> every(Duration duration) {
        return MultiCollector.multi(upstream, validate(duration, "duration"));
    }

    /**
     * Creates a {@link Multi} that emits windows of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits {@link Multi multis} of {@code size} consecutive and non-overlapping items.
     * Each emitted {@code Multi} is completed once the last item is emitted.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the {@link Multi} emits the current Multi (and the
     * completion event) and sends the completion event. This last {@code Multi} may not contain {@code size} items.
     * If the upstream {@link Multi} sends the completion event before having emitted any event, the completion event is
     * propagated immediately.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param size the max number of item in each emitted {@link Multi}, must be positive
     * @return a Multi emitting multis of at most {@code size} items from the upstream Multi.
     */
    public Multi<Multi<T>> of(int size) {
        return MultiCollector.multi(upstream, positive(size, "size"));
    }

    /**
     * Creates a {@link Multi} that emits windows of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits {@link Multi multis} every {@code skip} items, each containing {@code size}
     * items. Each emitted {@code Multi} is completed once the last item is emitted.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the {@link Multi} emits the current Multi (and the
     * completion event) and sends the completion event. This last {@code Multi} may not contain {@code size} items.
     * If the upstream {@link Multi} sends the completion event before having emitted any event, the completion event is
     * propagated immediately.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param size the max number of item in each emitted {@link Multi}, must be positive
     * @param skip the number of items skipped before starting a new multi. If {@code skip} and {@code size} are equal,
     *        this operation is similar to {@link #of(int)}. Must be positive and non-0
     * @return a Multi emitting multis of at most {@code size} items from the upstream Multi.
     */
    public Multi<Multi<T>> of(int size, int skip) {
        return MultiCollector.multi(upstream, positive(size, "size"), positive(skip, "skip"));
    }

}
