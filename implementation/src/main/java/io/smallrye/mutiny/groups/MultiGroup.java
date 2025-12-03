package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.GroupedMulti;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiGroupByOp;

public class MultiGroup<T> {

    private final Multi<T> upstream;

    public MultiGroup(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Splits the upstream {@code Multi} into {@link java.util.List} of items and emits these lists.
     * The returned object configures how the split is made.
     *
     * @return the split configuration
     */
    @CheckReturnValue
    public MultiGroupIntoLists<T> intoLists() {
        return new MultiGroupIntoLists<>(upstream);
    }

    /**
     * Splits the upstream {@code Multi} into {@link Multi} of items and emits these {@link Multi}. It transforms the
     * upstream {@link Multi} into a {@code Multi<Multi<T>>}, where each emitted multi contains items from the upstream.
     * <p>
     * The returned object configures how the split is made.
     *
     * @return the split configuration
     */
    @CheckReturnValue
    public MultiGroupIntoMultis<T> intoMultis() {
        return new MultiGroupIntoMultis<>(upstream);
    }

    // TODO grouping can also have prefetch and failure collection delay.

    /**
     * Groups items emitted by the upstream {@code Multi} based on a key extracted by the {@code keyMapper} function.
     * <p>
     * The returned {@code Multi<GroupedMulti<K, T>>} emits {@link GroupedMulti} instances, where each represents
     * a group of items sharing the same key. Items are distributed to groups as they are emitted by the upstream.
     * <p>
     * Each {@link GroupedMulti} provides a {@link GroupedMulti#key()} method that returns the key for that group.
     * Groups are created dynamically as new keys are discovered in the stream.
     *
     * @param keyMapper the function to extract the key from each item, must not be {@code null}
     * @param <K> the type of the key
     * @return a {@code Multi} emitting {@link GroupedMulti} instances
     */
    @CheckReturnValue
    public <K> Multi<GroupedMulti<K, T>> by(Function<? super T, ? extends K> keyMapper) {
        return by(keyMapper, Infrastructure.getBufferSizeS());
    }

    /**
     * Groups items emitted by the upstream {@code Multi} based on a key extracted by the {@code keyMapper} function,
     * while transforming items using the {@code valueMapper} function.
     * <p>
     * The returned {@code Multi<GroupedMulti<K, V>>} emits {@link GroupedMulti} instances, where each represents
     * a group of transformed items sharing the same key.
     * <p>
     *
     * @param keyMapper the function to extract the key from each item, must not be {@code null}
     * @param valueMapper the function to transform each item, must not be {@code null}
     * @param <K> the type of the key
     * @param <V> the type of the transformed value
     * @return a {@code Multi} emitting {@link GroupedMulti} instances
     */
    @CheckReturnValue
    public <K, V> Multi<GroupedMulti<K, V>> by(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Function<? super T, ? extends K> k = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        Function<? super T, ? extends V> v = Infrastructure.decorate(nonNull(valueMapper, "valueMapper"));
        return by(k, v, Infrastructure.getBufferSizeS());
    }

    /**
     * Groups items emitted by the upstream {@code Multi} based on a key extracted by the {@code keyMapper} function,
     * with configurable prefetch buffer size.
     * <p>
     * The returned {@code Multi<GroupedMulti<K, T>>} emits {@link GroupedMulti} instances, where each represents
     * a group of items sharing the same key. Items are distributed to groups as they are emitted by the upstream.
     * <p>
     * The {@code prefetch} parameter controls how many items can be buffered for groups that are not yet subscribed to
     * or are waiting for backpressure.
     * <p>
     *
     * @param keyMapper the function to extract the key from each item, must not be {@code null}
     * @param prefetch the prefetch buffer size, must be positive
     * @param <K> the type of the key
     * @return a {@code Multi} emitting {@link GroupedMulti} instances
     */
    @CheckReturnValue
    public <K> Multi<GroupedMulti<K, T>> by(Function<? super T, ? extends K> keyMapper, long prefetch) {
        positive(prefetch, "prefetch");
        Function<? super T, ? extends K> mapper = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        return Infrastructure.onMultiCreation(new MultiGroupByOp<>(upstream, mapper, x -> x, prefetch));
    }

    /**
     * Groups items emitted by the upstream {@code Multi} based on a key extracted by the {@code keyMapper} function,
     * while transforming items using the {@code valueMapper} function, with configurable prefetch buffer size.
     * <p>
     * The returned {@code Multi<GroupedMulti<K, V>>} emits {@link GroupedMulti} instances, where each represents
     * a group of transformed items sharing the same key.
     * <p>
     * The {@code prefetch} parameter controls how many items can be buffered for groups that are not yet subscribed to
     * or are waiting for backpressure.
     * <p>
     *
     * @param keyMapper the function to extract the key from each item, must not be {@code null}
     * @param valueMapper the function to transform each item, must not be {@code null}
     * @param prefetch the prefetch buffer size, must be positive
     * @param <K> the type of the key
     * @param <V> the type of the transformed value
     * @return a {@code Multi} emitting {@link GroupedMulti} instances
     */
    @CheckReturnValue
    public <K, V> Multi<GroupedMulti<K, V>> by(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper, long prefetch) {
        positive(prefetch, "prefetch");
        Function<? super T, ? extends K> k = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        Function<? super T, ? extends V> v = Infrastructure.decorate(nonNull(valueMapper, "valueMapper"));
        return Infrastructure.onMultiCreation(new MultiGroupByOp<>(upstream, k, v, prefetch));
    }
}
