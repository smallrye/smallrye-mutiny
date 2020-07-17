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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.BuiltinConverters;

public class UniConvert<T> {

    private final Uni<T> upstream;

    public UniConvert(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Transforms this {@link Uni} into a type using the provided converter.
     *
     * @param converter the converter function
     * @return an instance of R
     * @param <R> the result type
     * @throws RuntimeException if the conversion fails.
     */
    public <R> R with(Function<Uni<T>, R> converter) {
        nonNull(converter, "converter");
        return converter.apply(upstream);
    }

    public CompletionStage<T> toCompletionStage() {
        return with(BuiltinConverters.toCompletionStage());
    }

    public CompletableFuture<T> toCompletableFuture() {
        return with(BuiltinConverters.toCompletableFuture());
    }

    public Publisher<T> toPublisher() {
        return with(BuiltinConverters.toPublisher());
    }

}
