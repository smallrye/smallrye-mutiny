package io.smallrye.reactive.unimulti.operators;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.reactivex.Flowable;
import io.reactivex.flowables.GroupedFlowable;
import io.smallrye.reactive.unimulti.GroupedMulti;
import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.operators.flowable.FlowableCollector;

public class MultiCollector {

    private MultiCollector() {
        // avoid direct instantiation.
    }

    public static <T> Uni<T> first(Multi<T> upstream) {
        return Uni.createFrom().multi(upstream);
    }

    public static <T> Uni<T> last(Multi<T> upstream) {
        return Uni.createFrom().publisher(getFlowable(upstream).lastElement().toFlowable());
    }

    public static <T> Uni<List<T>> list(Multi<T> upstream) {
        return collector(upstream, Collectors.toList());
    }

    public static <T, A, R> Uni<R> collector(Multi<T> upstream, Collector<? super T, A, ? extends R> collector) {
        FlowableCollector<? super T, A, ? extends R> f = new FlowableCollector<>(upstream, collector);
        return Uni.createFrom().publisher(f);
    }

    static <T> Flowable<T> getFlowable(Multi<T> upstream) {
        if (upstream instanceof AbstractMulti) {
            return ((AbstractMulti<T>) upstream).flowable();
        }
        return Flowable.fromPublisher(upstream);
    }

    public static <R, T> Uni<R> collectInto(Multi<T> upstream, Supplier<R> producer,
            BiConsumer<R, ? super T> combinator) {
        Collector<? super T, R, R> collector = Collector.of(producer, combinator, (BinaryOperator<R>) (r, r2) -> r,
                Collector.Characteristics.IDENTITY_FINISH);
        return collector(upstream, collector);
    }

    public static <K, T> Uni<Map<K, T>> map(Multi<T> upstream, Function<? super T, ? extends K> keyMapper) {
        return map(upstream, keyMapper, Function.identity());
    }

    public static <K, V, T> Uni<Map<K, V>> map(Multi<T> upstream, Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        return collector(upstream, Collectors.toMap(keyMapper, valueMapper));
    }

    public static <K, V, R> Uni<Map<K, Collection<V>>> multimap(Multi<R> upstream,
            Function<? super R, ? extends K> keyMapper,
            Function<? super R, ? extends V> valueMapper) {
        return collector(upstream, Collectors.toMap(
                keyMapper,
                res -> {
                    List<V> list = new ArrayList<>();
                    V mapped = valueMapper.apply(res);
                    list.add(mapped);
                    return list;
                },
                (vs, vs2) -> {
                    vs.addAll(vs2);
                    return vs;
                }));
    }

    public static <T> Multi<List<T>> list(Multi<T> upstream, Duration timeWindow) {
        return Multi.createFrom().publisher(getFlowable(upstream).buffer(timeWindow.toMillis(), TimeUnit.MILLISECONDS));
    }

    public static <T> Multi<List<T>> list(Multi<T> upstream, int size) {
        return Multi.createFrom().publisher(getFlowable(upstream).buffer(size));
    }

    public static <T> Multi<List<T>> list(Multi<T> upstream, int size, int skip) {
        return Multi.createFrom().publisher(getFlowable(upstream).buffer(size, skip));
    }

    public static <T> Multi<Multi<T>> multi(Multi<T> upstream, Duration timeWindow) {
        return Multi.createFrom().publisher(getFlowable(upstream)
                .window(timeWindow.toMillis(), TimeUnit.MILLISECONDS)
                .map(f -> Multi.createFrom().publisher(f)));
    }

    public static <T> Multi<Multi<T>> multi(Multi<T> upstream, int size) {
        return Multi.createFrom().publisher(getFlowable(upstream)
                .window(size)
                .map(f -> Multi.createFrom().publisher(f)));
    }

    public static <T> Multi<Multi<T>> multi(Multi<T> upstream, int size, int skip) {
        return Multi.createFrom().publisher(getFlowable(upstream)
                .window(size, skip)
                .map(f -> Multi.createFrom().publisher(f)));
    }

    public static <K, V, T> Multi<GroupedMulti<K, V>> groupBy(Multi<T> upstream,
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Flowable<? extends GroupedFlowable<? extends K, ? extends V>> groups;
        if (valueMapper == null) {
            groups = getFlowable(upstream).groupBy(keyMapper::apply, x -> (V) x);
        } else {
            groups = getFlowable(upstream).groupBy(keyMapper::apply, valueMapper::apply);
        }
        return Multi.createFrom().publisher(groups).onItem().mapToItem(gf -> new DefaultGroupedMulti<>(gf));
    }
}
