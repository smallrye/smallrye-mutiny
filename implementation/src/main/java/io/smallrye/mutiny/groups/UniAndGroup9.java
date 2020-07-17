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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Functions;
import io.smallrye.mutiny.tuples.Tuple9;
import io.smallrye.mutiny.tuples.Tuples;

public class UniAndGroup9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends UniAndGroupIterable<T1> {

    public UniAndGroup9(Uni<? extends T1> source, Uni<? extends T2> o1, Uni<? extends T3> o2, // NOSONAR
            Uni<? extends T4> o3, Uni<? extends T5> o4, Uni<? extends T6> o5,
            Uni<? extends T7> o6, Uni<? extends T8> o7, Uni<? extends T9> o8) {
        super(source, Arrays.asList(o1, o2, o3, o4, o5, o6, o7, o8));
    }

    @Override
    public UniAndGroup9<T1, T2, T3, T4, T5, T6, T7, T8, T9> collectFailures() {
        super.collectFailures();
        return this;
    }

    public Uni<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> asTuple() {
        return combinedWith(Tuple9::of);
    }

    @SuppressWarnings("unchecked")
    public <O> Uni<O> combinedWith(Functions.Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, O> combinator) {
        Function<List<?>, O> function = list -> {
            Tuples.ensureArity(list, 9);

            return combinator.apply(
                    (T1) list.get(0),
                    (T2) list.get(1),
                    (T3) list.get(2),
                    (T4) list.get(3),
                    (T5) list.get(4),
                    (T6) list.get(5),
                    (T7) list.get(6),
                    (T8) list.get(7),
                    (T9) list.get(8));
        };
        return super.combinedWith(function);
    }

}
