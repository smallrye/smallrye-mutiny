package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

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

    @CheckReturnValue
    public <K> Multi<GroupedMulti<K, T>> by(Function<? super T, ? extends K> keyMapper) {
        Function<? super T, ? extends K> mapper = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        return Infrastructure.onMultiCreation(new MultiGroupByOp<>(upstream, mapper, x -> x));
    }

    @CheckReturnValue
    public <K, V> Multi<GroupedMulti<K, V>> by(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Function<? super T, ? extends K> k = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        Function<? super T, ? extends V> v = Infrastructure.decorate(nonNull(valueMapper, "valueMapper"));
        return Infrastructure.onMultiCreation(new MultiGroupByOp<>(upstream, k, v));
    }
}
