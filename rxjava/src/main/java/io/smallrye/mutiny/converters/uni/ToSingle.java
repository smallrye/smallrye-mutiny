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
package io.smallrye.mutiny.converters.uni;

import java.util.Optional;
import java.util.function.Function;

import io.reactivex.Single;
import io.smallrye.mutiny.Uni;

public class ToSingle<T> implements Function<Uni<T>, Single<Optional<T>>> {
    public static final ToSingle INSTANCE = new ToSingle();

    private ToSingle() {
        // Avoid direct instantiation
    }

    public static <R> ToSingleWithDefault<R> withDefault(R defaultValue) {
        return new ToSingleWithDefault<>(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <R> ToSingleFailOnNull<R> failOnNull() {
        return ToSingleFailOnNull.INSTANCE;
    }

    @Override
    public Single<Optional<T>> apply(Uni<T> uni) {
        return Single.fromPublisher(uni.map(Optional::ofNullable).convert().toPublisher());
    }

}
