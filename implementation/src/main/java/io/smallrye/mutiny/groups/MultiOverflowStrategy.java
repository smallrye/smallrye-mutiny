package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.function.Consumer;
import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.overflow.MultiOnOverflowBufferOp;
import io.smallrye.mutiny.operators.multi.overflow.MultiOnOverflowDropItemsOp;
import io.smallrye.mutiny.operators.multi.overflow.MultiOnOverflowKeepLastOp;
import io.smallrye.mutiny.subscription.BackPressureFailure;

public class MultiOverflowStrategy<T> {

    private final Multi<T> upstream;
    private final Consumer<T> dropConsumer;
    private final Function<T, Uni<?>> dropUniMapper;

    public MultiOverflowStrategy(Multi<T> upstream, Consumer<T> dropConsumer, Function<T, Uni<?>> dropUniMapper) {
        // No need for null checks since MultiOverflow must have performed them
        this.upstream = upstream;
        this.dropConsumer = dropConsumer;
        this.dropUniMapper = dropUniMapper;
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop the item.
     *
     * @return the new multi
     */
    @CheckReturnValue
    public Multi<T> drop() {
        return Infrastructure.onMultiCreation(new MultiOnOverflowDropItemsOp<>(upstream, dropConsumer, dropUniMapper));
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to use a <strong>bounded</strong>
     * buffer of the default size to store the items until they are consumed.
     *
     * @return the new multi
     * @see Infrastructure#setMultiOverflowDefaultBufferSize(int)
     */
    @CheckReturnValue
    public Multi<T> buffer() {
        return buffer(Infrastructure.getMultiOverflowDefaultBufferSize());
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to use a buffer of size {@code size}
     * to store the items until they are consumed. When the buffer is full, a {@link BackPressureFailure} is propagated
     * downstream and the upstream source is cancelled.
     *
     * @param size the size of the buffer, must be strictly positive
     * @return the new multi
     */
    @CheckReturnValue
    public Multi<T> buffer(int size) {
        return Infrastructure.onMultiCreation(new MultiOnOverflowBufferOp<>(upstream, positive(size, "size"),
                false, dropConsumer, dropUniMapper));
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to use an <strong>unbounded</strong>
     * buffer to store the items until they are consumed.
     *
     * @return the new multi
     */
    @CheckReturnValue
    public Multi<T> bufferUnconditionally() {
        return Infrastructure.onMultiCreation(new MultiOnOverflowBufferOp<>(upstream,
                Infrastructure.getMultiOverflowDefaultBufferSize(), true, dropConsumer, dropUniMapper));
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop all previously buffered items.
     *
     * @return the new multi
     */
    @CheckReturnValue
    public Multi<T> dropPreviousItems() {
        return Infrastructure.onMultiCreation(new MultiOnOverflowKeepLastOp<>(upstream, dropConsumer, dropUniMapper));
    }
}
