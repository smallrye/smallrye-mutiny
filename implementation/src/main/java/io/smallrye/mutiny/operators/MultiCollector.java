/*
 * Copyright (c) 2019-2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiBufferOp;
import io.smallrye.mutiny.operators.multi.MultiBufferWithTimeoutOp;
import io.smallrye.mutiny.operators.multi.MultiCollectorOp;
import io.smallrye.mutiny.operators.multi.MultiLastItemOp;

public class MultiCollector {

    private MultiCollector() {
        // avoid direct instantiation.
    }

    public static <T> Uni<T> first(Multi<T> upstream) {
        return Uni.createFrom().multi(upstream);
    }

    public static <T> Uni<T> last(Multi<T> upstream) {
        return Uni.createFrom().publisher(Infrastructure.onMultiCreation(new MultiLastItemOp<>(upstream)));
    }

    public static <T> Uni<List<T>> list(Multi<T> upstream) {
        return collector(upstream, Collectors.toList(), false);
    }

    public static <T, A, R> Uni<R> collector(Multi<T> upstream, Collector<? super T, A, ? extends R> collector,
            boolean acceptNullAsInitialValue) {
        Multi<R> multi = Infrastructure.onMultiCreation(new MultiCollectorOp<>(upstream, collector, acceptNullAsInitialValue));
        return Uni.createFrom().publisher(multi);
    }

    public static <R, T> Uni<R> collectInto(Multi<T> upstream, Supplier<R> producer,
            BiConsumer<R, ? super T> combinator) {
        Collector<? super T, R, R> collector = Collector.of(producer, combinator, (BinaryOperator<R>) (r, r2) -> r,
                Collector.Characteristics.IDENTITY_FINISH);
        return collector(upstream, collector, false);
    }

    public static <K, T> Uni<Map<K, T>> map(Multi<T> upstream, Function<? super T, ? extends K> keyMapper) {
        return map(upstream, keyMapper, Function.identity());
    }

    public static <K, V, T> Uni<Map<K, V>> map(Multi<T> upstream, Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        return collector(upstream, Collectors.toMap(keyMapper, valueMapper), false);
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
                }), false);
    }

    public static <T> Multi<List<T>> list(Multi<T> upstream, Duration timeWindow) {
        return Infrastructure.onMultiCreation(new MultiBufferWithTimeoutOp<>(upstream, Integer.MAX_VALUE, timeWindow,
                Infrastructure.getDefaultWorkerPool()));
    }

    public static <T> Multi<List<T>> list(Multi<T> upstream, int size) {
        return Infrastructure.onMultiCreation(new MultiBufferOp<>(upstream, size, size));
    }

    public static <T> Multi<List<T>> list(Multi<T> upstream, int size, int skip) {
        return Infrastructure.onMultiCreation(new MultiBufferOp<>(upstream, size, skip));
    }

}
