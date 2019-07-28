package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.MultiCollector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiCollect<T> {

    private final Multi<T> upstream;

    public MultiCollect(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    public Uni<T> first() {
        return MultiCollector.first(upstream);
    }

    public Uni<T> last() {
        return MultiCollector.last(upstream);
    }


    public Uni<List<T>> asList() {
        return MultiCollector.list(upstream);
    }

    public <X, A> Uni<X> with(Collector<? super T, A, ? extends X> collector) {
        return MultiCollector.collector(upstream, collector);
    }

    public <X> Uni<X> in(Supplier<X> producer, BiConsumer<X, T> accumulator) {
        return MultiCollector.collectInto(upstream, producer, accumulator);
    }

    public <K> Uni<Map<K, T>> asMap(Function<? super T, ? extends K> keyMapper) {
        return asMap(keyMapper, Function.identity());
    }

    public <K, V> Uni<Map<K, V>> asMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return MultiCollector.map(upstream, keyMapper, valueMapper);
    }

    public <K, V> Uni<Map<K, Collection<V>>> asMultiMap(Function<? super T, ? extends K> keyMapper,
                                                        Function<? super T, ? extends V> valueMapper) {
        return MultiCollector.multimap(upstream, keyMapper, valueMapper);
    }

    public <K> Uni<Map<K, Collection<T>>> asMultiMap(Function<? super T, ? extends K> keyMapper) {
        return MultiCollector.multimap(upstream, keyMapper, Function.identity());
    }
}
