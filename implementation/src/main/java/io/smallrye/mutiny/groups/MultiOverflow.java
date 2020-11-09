package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.overflow.MultiOnOverflowBufferOp;
import io.smallrye.mutiny.operators.multi.overflow.MultiOnOverflowDropItemsOp;
import io.smallrye.mutiny.operators.multi.overflow.MultiOnOverflowKeepLastOp;
import io.smallrye.mutiny.subscription.BackPressureFailure;

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
        return buffer(128);
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
        return Infrastructure
                .onMultiCreation(new MultiOnOverflowBufferOp<>(upstream, ParameterValidation.positive(size, "size"),
                        false,
                        x -> {
                            // do nothing
                        }, null));
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop the item.
     *
     * @return the new multi
     */
    public Multi<T> drop() {
        return Infrastructure.onMultiCreation(new MultiOnOverflowDropItemsOp<>(upstream));
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop the item.
     *
     * @param callback a callback invoked when an item is dropped. The callback receives the item. Must not be
     *        {@code null}
     * @return the new multi
     */
    public Multi<T> drop(Consumer<T> callback) {
        return Infrastructure
                .onMultiCreation(new MultiOnOverflowDropItemsOp<>(upstream, nonNull(callback, "callback")));
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop all previously buffered items.
     *
     * @return the new multi
     */
    public Multi<T> dropPreviousItems() {
        return Infrastructure.onMultiCreation(new MultiOnOverflowKeepLastOp<>(upstream, null, null));
    }

    public MultiOverflowStrategy<T> invoke(Consumer<T> consumer) {
        return new MultiOverflowStrategy<>(upstream, nonNull(consumer, "consumer"), null);
    }

    public MultiOverflowStrategy<T> invoke(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        return invoke(ignored -> actual.run());
    }

    public MultiOverflowStrategy<T> call(Supplier<Uni<?>> supplier) {
        Supplier<Uni<?>> actual = nonNull(supplier, "supplier");
        return call(ignored -> actual.get());
    }

    public MultiOverflowStrategy<T> call(Function<T, Uni<?>> mapper) {
        return new MultiOverflowStrategy<>(upstream, null, nonNull(mapper, "mapper"));
    }
}
