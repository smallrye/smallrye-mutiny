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

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiConvert<T> {

    private final Multi<T> upstream;

    public MultiConvert(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Transforms this {@link Multi} into a type using the provided converter.
     *
     * @param converter the converter function
     * @param <R> the type produced by the converter
     * @return an instance of R
     * @throws RuntimeException if the conversion fails.
     */
    public <R> R with(Function<Multi<T>, R> converter) {
        nonNull(converter, "converter");
        return converter.apply(upstream);
    }

    /**
     * Converts the {@link Multi} into a {@link Publisher}.
     * <p>
     * Basically, this method returns the {@link Multi} as it is, as {@link Multi} implements {@link Publisher}.
     *
     * @return the publisher
     */
    public Publisher<T> toPublisher() {
        return upstream;
    }
}
