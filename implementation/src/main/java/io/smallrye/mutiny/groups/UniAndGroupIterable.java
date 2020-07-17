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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniAndCombination;

public class UniAndGroupIterable<T1> {

    private final Uni<? extends T1> source;
    private final List<? extends Uni<?>> unis;

    private boolean collectFailures;

    public UniAndGroupIterable(Iterable<? extends Uni<?>> iterable) {
        this(null, iterable, false);
    }

    public UniAndGroupIterable(Uni<? extends T1> source, Iterable<? extends Uni<?>> iterable) {
        this(source, iterable, false);
    }

    @SuppressWarnings("unchecked")
    public UniAndGroupIterable(Uni<? extends T1> source, Iterable<? extends Uni<?>> iterable, boolean collectFailures) {
        this.source = source;
        List<? extends Uni<?>> others;
        if (iterable instanceof List) {
            others = (List) iterable;
        } else {
            others = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
        }
        this.unis = others;
        this.collectFailures = collectFailures;
    }

    public UniAndGroupIterable<T1> collectFailures() {
        collectFailures = true;
        return this;
    }

    public <O> Uni<O> combinedWith(Function<List<?>, O> function) {
        return Infrastructure
                .onUniCreation(new UniAndCombination<>(source, unis, nonNull(function, "function"), collectFailures));
    }

    /**
     * Discards the items emitted by the combined {@link Uni unis}, and just emits {@code null} when all the
     * {@link Uni unis} have completed successfully. In the case of failure, the failure is propagated.
     *
     * @return the {@code Uni Uni<Void>} emitting {@code null} when all the {@link Uni unis} have completed, or propagating
     *         the failure.
     */
    public Uni<Void> discardItems() {
        return Infrastructure
                .onUniCreation(new UniAndCombination<>(source, unis, x -> null, collectFailures));
    }

}
