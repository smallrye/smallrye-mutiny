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

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;
import reactor.core.publisher.Flux;

public class FromFlux<T> implements UniConverter<Flux<T>, T> {

    public final static FromFlux INSTANCE = new FromFlux();

    private FromFlux() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Flux<T> instance) {
        return Uni.createFrom().publisher(instance);
    }
}
