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

import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;

public class FromMaybe<T> implements UniConverter<Maybe<T>, T> {

    public static final FromMaybe INSTANCE = new FromMaybe();

    private FromMaybe() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Maybe<T> instance) {
        return Uni.createFrom().emitter(sink -> {
            Disposable disposable = instance.subscribe(
                    sink::complete,
                    sink::fail,
                    () -> sink.complete(null));

            sink.onTermination(() -> {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            });
        });
    }
}
