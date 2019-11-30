package io.smallrye.mutiny.operators;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.smallrye.mutiny.GroupedMulti;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.*;

public class MultiCollector {

    private MultiCollector() {
        // avoid direct instantiation.
    }

    public static <T> Uni<T> first(Multi<T> upstream) {
        return Uni.createFrom().multi(upstream);
    }

    public static <T> Uni<T> last(Multi<T> upstream) {
        return Uni.createFrom().publisher(new MultiLastItemOp<>(upstream));
    }

    public static <T> Uni<List<T>> list(Multi<T> upstream) {
        return collector(upstream, Collectors.toList());
    }

    public static <T, A, R> Uni<R> collector(Multi<T> upstream, Collector<? super T, A, ? extends R> collector) {
        MultiCollectorOp<? super T, A, ? extends R> f = new MultiCollectorOp<>(upstream, collector);
        return Uni.createFrom().publisher(f);
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
        return new MultiBufferWithTimeoutOp<>(upstream, Integer.MAX_VALUE, timeWindow,
                Infrastructure.getDefaultWorkerPool());
    }

    public static <T> Multi<List<T>> list(Multi<T> upstream, int size) {
        return new MultiBufferOp<>(upstream, size, size);
    }

    public static <T> Multi<List<T>> list(Multi<T> upstream, int size, int skip) {
        return new MultiBufferOp<>(upstream, size, skip);
    }

    public static <T> Multi<Multi<T>> multi(Multi<T> upstream, Duration timeWindow) {
        return new MultiWindowOnDurationOp<>(upstream, timeWindow, Infrastructure.getDefaultWorkerPool());
    }

    public static <T> Multi<Multi<T>> multi(Multi<T> upstream, int size) {
        return new MultiWindowOp<>(upstream, size, size);
    }

    public static <T> Multi<Multi<T>> multi(Multi<T> upstream, int size, int skip) {
        return new MultiWindowOp<>(upstream, size, skip);
    }

    public static <K, V, T> Multi<GroupedMulti<K, V>> groupBy(Multi<T> upstream,
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        if (valueMapper == null) {
            //noinspection unchecked
            return new MultiGroupByOp<>(upstream, keyMapper, x -> (V) x);
        } else {
            return new MultiGroupByOp<>(upstream, keyMapper, valueMapper);
        }
    }
}
