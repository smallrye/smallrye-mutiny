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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOrCombination;

public class UniOr<T> {

    private final Uni<T> upstream;

    public UniOr(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    public Uni<T> uni(Uni<T> other) {
        return unis(upstream, other);
    }

    @SafeVarargs
    public final Uni<T> unis(Uni<T>... other) {
        List<Uni<T>> list = new ArrayList<>();
        list.add(upstream);
        list.addAll(Arrays.asList(other));
        return Infrastructure.onUniCreation(new UniOrCombination<>(list));
    }

}
