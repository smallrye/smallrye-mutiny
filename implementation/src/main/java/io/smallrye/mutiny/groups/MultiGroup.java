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
package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;

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
        Function<? super T, ? extends K> mapper = nonNull(keyMapper, "keyMapper");
        return Infrastructure.onMultiCreation(new MultiGroupByOp<>(upstream, mapper, x -> x));
    }

    public <K, V> Multi<GroupedMulti<K, V>> by(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Function<? super T, ? extends K> k = nonNull(keyMapper, "keyMapper");
        Function<? super T, ? extends V> v = nonNull(valueMapper, "valueMapper");
        return Infrastructure.onMultiCreation(new MultiGroupByOp<>(upstream, k, v));
    }
}
