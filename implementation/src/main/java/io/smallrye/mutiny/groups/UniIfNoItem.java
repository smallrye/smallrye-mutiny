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
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;

import io.smallrye.mutiny.Uni;

public class UniIfNoItem<T> {

    private final Uni<T> upstream;

    public UniIfNoItem(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Configures the timeout duration.
     *
     * @param timeout the timeout, must not be {@code null}, must be strictly positive.
     * @return a new {@link UniIfNoItem}
     */
    public UniOnTimeout<T> after(Duration timeout) {
        return new UniOnTimeout<>(upstream, validate(timeout, "timeout"), null);
    }

}
