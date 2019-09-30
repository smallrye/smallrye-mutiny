package io.smallrye.reactive.unimulti.groups;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.positive;

import java.util.function.Consumer;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.operators.Overflows;
import io.smallrye.reactive.unimulti.subscription.BackPressureFailure;

public class MultiOverflow<T> {
    private final Multi<T> upstream;

    public MultiOverflow(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to use an <strong>unbounded</strong>
     * buffer to store the items until they are consumed.
     *
     * @return the new multi
     */
    public Multi<T> buffer() {
        return Overflows.buffer(upstream);
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to use a buffer of size {@code size}
     * to store the items until they are consumed. When the buffer is full, a {@link BackPressureFailure} is propagated
     * downstream and the upstream source is cancelled.
     *
     * @param size the size of the buffer, must be strictly positive
     * @return the new multi
     */
    public Multi<T> buffer(int size) {
        return Overflows.buffer(upstream, positive(size, "size"));
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop the item.
     *
     * @return the new multi
     */
    public Multi<T> drop() {
        return Overflows.dropNewItems(upstream);
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop the item.
     *
     * @param callback a callback invoked when an item is dropped. The callback receives the item. Must not be
     *        {@code null}
     * @return the new multi
     */
    public Multi<T> drop(Consumer<T> callback) {
        return Overflows.dropNewItems(upstream, nonNull(callback, "callback"));
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop all previously buffered items.
     *
     * @return the new multi
     */
    public Multi<T> dropPreviousItems() {
        return Overflows.dropBufferedItems(upstream);
    }
}
