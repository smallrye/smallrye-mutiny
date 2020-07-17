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
package io.smallrye.mutiny.streams.operators;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.streams.Engine;

/**
 * Factory to create {@link PublisherStage} instances.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FunctionalInterface
public interface PublisherStageFactory<T extends Stage> {

    /**
     * Creates the instance.
     *
     * @param engine the reactive engine
     * @param stage the stage
     * @param <O> output data
     * @return the created processing stage, should never be {@code null}
     */
    <O> PublisherStage<O> create(Engine engine, T stage);

}
