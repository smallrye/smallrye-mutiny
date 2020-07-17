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

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuples;

/**
 * Configures the combination of 2 {@link Uni unis}.
 *
 * @param <T1> the type of item of the first {@link Uni}
 * @param <T2> the type of item of the second {@link Uni}
 */
public class UniAndGroup2<T1, T2> extends UniAndGroupIterable<T1> {

    public UniAndGroup2(Uni<? extends T1> source, Uni<? extends T2> other) {
        super(source, Collections.singletonList(other), false);
    }

    /**
     * Configure the processing to wait until all the {@link Uni unis} to fire an event (item or failure) before
     * firing the failure. If several failures have been collected, a {@link CompositeException} is fired wrapping
     * the different failures.
     *
     * @return the current {@link UniAndGroup2}
     */
    public UniAndGroup2<T1, T2> collectFailures() {
        super.collectFailures();
        return this;
    }

    /**
     * @return the resulting {@link Uni}. The items are combined into a {@link Tuple2 Tuple2&lt;T1, T2&gt;}.
     */
    public Uni<Tuple2<T1, T2>> asTuple() {
        return combinedWith(Tuple2::of);
    }

    /**
     * Creates the resulting {@link Uni}. The items are combined using the given combinator function.
     *
     * @param combinator the combinator function, must not be {@code null}
     * @param <O> the type of item
     * @return the resulting {@link Uni}. The items are combined into a {@link Tuple2 Tuple2&lt;T1, T2&gt;}.
     */
    @SuppressWarnings("unchecked")
    public <O> Uni<O> combinedWith(BiFunction<T1, T2, O> combinator) {
        Function<List<?>, O> function = list -> {
            Tuples.ensureArity(list, 2);
            T1 item1 = (T1) list.get(0);
            T2 item2 = (T2) list.get(1);
            return combinator.apply(item1, item2);
        };
        return super.combinedWith(function);
    }

}
