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
package io.smallrye.mutiny.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.PublisherStage;
import io.smallrye.mutiny.streams.operators.PublisherStageFactory;

/**
 * Implementation of the {@link Stage.Of} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FromIterableStageFactory implements PublisherStageFactory<Stage.Of> {

    @SuppressWarnings("unchecked")
    @Override
    public <O> PublisherStage<O> create(Engine engine, Stage.Of stage) {
        Iterable<O> elements = (Iterable<O>) Objects.requireNonNull(Objects.requireNonNull(stage).getElements());
        return () -> Multi.createFrom().iterable(elements);
    }
}
