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
package io.smallrye.mutiny.converters.multi;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.reactivex.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.uni.UniRxConverters;
import io.smallrye.mutiny.helpers.ParameterValidation;

public class ToSingleFailOnNull<T> implements Function<Multi<T>, Single<T>> {
    private final Supplier<? extends Throwable> supplier;

    ToSingleFailOnNull(Supplier<? extends Throwable> supplier) {
        this.supplier = ParameterValidation.nonNull(supplier, "supplier");
    }

    @Override
    public Single<T> apply(Multi<T> multi) {
        return multi.collectItems().first()
                .onItem().ifNull().failWith(supplier)
                .convert().with(UniRxConverters.toSingle())
                .map(Optional::get);
    }
}
