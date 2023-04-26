package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import java.time.Duration;
import java.util.List;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiBufferOp;
import io.smallrye.mutiny.operators.multi.MultiBufferWithTimeoutOp;

public class MultiGroupIntoLists<T> {

    private final Multi<T> upstream;

    public MultiGroupIntoLists(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Creates a {@link Multi} that emits lists of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits connected, non-overlapping lists, each of a fixed duration specified by the
     * {@code duration} parameter. If, during the configured time window, no items are emitted by the upstream
     * {@link Multi}, nothing is emitted downstream.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the resulting {@link Multi} emits the current list
     * and propagates the completion event.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param duration the period of time each list collects items before it is emitted and replaced with a new
     *        list. Must be non {@code null} and positive.
     * @return a Multi that emits every {@code duration} with the items emitted by the upstream multi during the time
     *         window.
     */
    @CheckReturnValue
    public Multi<List<T>> every(Duration duration) {
        return Infrastructure.onMultiCreation(new MultiBufferWithTimeoutOp<>(upstream, Integer.MAX_VALUE,
                validate(duration, "duration"),
                Infrastructure.getDefaultWorkerPool(),
                false));
    }

    /**
     * Creates a {@link Multi} that emits lists of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits connected, non-overlapping lists, each of a fixed duration specified by the
     * {@code duration} parameter. If, during the configured time window, no items are emitted by the upstream
     * {@link Multi}, an empty list is emitted by the returned {@link Multi} if {@code emitEmptyListIfNoItems} is set to
     * {@code true}.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the resulting {@link Multi} emits the current list
     * and propagates the completion event.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param duration the period of time each list collects items before it is emitted and replaced with a new
     *        list. Must be non {@code null} and positive.
     * @param emitEmptyListIfNoItems emits an empty list if no items from the upstream have been received during the
     *        time window
     * @return a Multi that emits every {@code duration} with the items emitted by the upstream multi during the time
     *         window.
     */
    @CheckReturnValue
    public Multi<List<T>> every(Duration duration, boolean emitEmptyListIfNoItems) {
        return Infrastructure.onMultiCreation(new MultiBufferWithTimeoutOp<>(upstream, Integer.MAX_VALUE,
                validate(duration, "duration"),
                Infrastructure.getDefaultWorkerPool(),
                emitEmptyListIfNoItems));
    }

    /**
     * Creates a {@link Multi} that emits lists of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits lists every {@code size} items.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the produced {@link Multi} emits the current list,
     * and sends the completion event. This last list may not contain {@code size} items. If the upstream {@link Multi}
     * sends the completion event before having emitted any event, the completion event is propagated immediately.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param size the size of each collected list, must be positive
     * @return a Multi emitting lists of at most {@code size} items from the upstream Multi.
     */
    @CheckReturnValue
    public Multi<List<T>> of(int size) {
        return Infrastructure.onMultiCreation(new MultiBufferOp<>(upstream, size, size));
    }

    /**
     * Creates a {@link Multi} that emits lists of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits lists every {@code skip} items, each containing {@code size} items.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the produced {@link Multi} emits the current list,
     * and sends the completion event. This last list may not contain {@code size} items. If the upstream {@link Multi}
     * * sends the completion event before having emitted any event, the completion event is propagated immediately.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param size the size of each collected list, must be positive and non-0
     * @param skip the number of items skipped before starting a new list. If {@code skip} and {@code size} are equal,
     *        this operation is similar to {@link #of(int)}. Must be positive and non-0
     * @return a Multi emitting lists for every {@code skip} items from the upstream Multi. Each list contains at most
     *         {@code size} items
     */
    @CheckReturnValue
    public Multi<List<T>> of(int size, int skip) {
        return Infrastructure.onMultiCreation(new MultiBufferOp<>(upstream,
                positive(size, "size"), positive(skip, "skip")));
    }

    /**
     * Creates a {@link Multi} that emits lists of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits lists every {@code maximumDelay} duration and splits them into lists of {@code size}
     * items.
     * Therefore, the list may not always contain {@code size} items but it is guaranteed to emit a list every
     * {@code maximumDelay}
     * duration, if at least one element was emitted by the upstream.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the produced {@link Multi} emits the current list,
     * and sends the completion event. This last list may also not contain {@code size} items. If the upstream {@link Multi}
     * sends the completion event before having emitted any event, the completion event is propagated immediately.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param size the maximum size of each collected list, must be positive
     * @param maximumDelay the maximum delay between the upstream emitting an item and a list being emitted by the returned
     *        {@link Multi}.
     * @return a Multi emitting lists of at most {@code size} items from the upstream Multi.
     */
    @CheckReturnValue
    public Multi<List<T>> of(int size, Duration maximumDelay) {
        return Infrastructure.onMultiCreation(new MultiBufferWithTimeoutOp<>(upstream, positive(size, "size"),
                validate(maximumDelay, "maximumDelay"), Infrastructure.getDefaultWorkerPool(), false));
    }
}
