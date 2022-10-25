package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
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
    @CheckReturnValue
    public Multi<T> bufferUnconditionally() {
        return new MultiOverflowStrategy<>(upstream, null, null).bufferUnconditionally();
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
        return new MultiOverflowStrategy<>(upstream, null, null).buffer();
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
        return new MultiOverflowStrategy<>(upstream, null, null).buffer(size);
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop the item.
     *
     * @return the new multi
     */
    @CheckReturnValue
    public Multi<T> drop() {
        return new MultiOverflowStrategy<>(upstream, null, null).drop();
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop all previously buffered items.
     *
     * @return the new multi
     */
    @CheckReturnValue
    public Multi<T> dropPreviousItems() {
        return new MultiOverflowStrategy<>(upstream, null, null).dropPreviousItems();
    }

    /**
     * Define an overflow callback.
     *
     * @param consumer the dropped item consumer, must not be {@code null}, must not return {@code null}
     * @return an object to select the overflow management strategy
     */
    @CheckReturnValue
    public MultiOverflowStrategy<T> invoke(Consumer<T> consumer) {
        Consumer<T> actual = Infrastructure.decorate(nonNull(consumer, "consumer"));
        return new MultiOverflowStrategy<>(upstream, actual, null);
    }

    /**
     * Define an overflow callback.
     *
     * @param callback a callback when overflow happens, must not be {@code null}, must not return {@code null}
     * @return an object to select the overflow management strategy
     */
    @CheckReturnValue
    public MultiOverflowStrategy<T> invoke(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        // Decoration happens in `invoke`
        return invoke(ignored -> actual.run());
    }

    /**
     * Define an overflow callback to a {@link Uni}.
     *
     * @param supplier a supplier of a {@link Uni}, must not be {@code null}, must not return {@code null}
     * @return an object to select the overflow management strategy
     */
    @CheckReturnValue
    public MultiOverflowStrategy<T> call(Supplier<Uni<?>> supplier) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return call(ignored -> actual.get());
    }

    /**
     * Define an overflow dropped item mapper to a {@link Uni}.
     *
     * @param mapper a mapper of a dropped item to a {@link Uni}, must not be {@code null}, must not return {@code null}
     * @return an object to select the overflow management strategy
     */
    @CheckReturnValue
    public MultiOverflowStrategy<T> call(Function<T, Uni<?>> mapper) {
        Function<T, Uni<?>> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return new MultiOverflowStrategy<>(upstream, null, actual);
    }
}
