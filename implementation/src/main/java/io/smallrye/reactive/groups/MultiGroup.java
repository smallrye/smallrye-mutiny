package io.smallrye.reactive.groups;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.function.Function;

import io.smallrye.reactive.GroupedMulti;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.MultiCollector;

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
    public MultiGroupIntoMultis<T> intoMultis() {
        return new MultiGroupIntoMultis<>(upstream);
    }

    // TODO grouping can also have prefetch and failure collection delay.

    public <K> Multi<GroupedMulti<K, T>> by(Function<? super T, ? extends K> keyMapper) {
        return MultiCollector.groupBy(upstream, nonNull(keyMapper, "keyMapper"), null);
    }

    public <K, V> Multi<GroupedMulti<K, V>> by(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        return MultiCollector.groupBy(upstream, nonNull(keyMapper, "keyMapper"), nonNull(valueMapper, "valueMapper"));
    }
}
