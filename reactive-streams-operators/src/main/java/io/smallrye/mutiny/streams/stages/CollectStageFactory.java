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
import java.util.concurrent.CompletionStage;
import java.util.stream.Collector;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.TerminalStage;
import io.smallrye.mutiny.streams.operators.TerminalStageFactory;

/**
 * Implement the {@link Stage.Collect} stage. It accumulates the result in a {@link Collector} and
 * redeems the last result.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CollectStageFactory implements TerminalStageFactory<Stage.Collect> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> TerminalStage<I, O> create(Engine engine, Stage.Collect stage) {
        Collector<I, Object, O> collector = (Collector<I, Object, O>) Objects.requireNonNull(stage).getCollector();
        Objects.requireNonNull(collector);
        return new CollectStage<>(collector);
    }

    private static class CollectStage<I, O> implements TerminalStage<I, O> {

        private final Collector<I, Object, O> collector;

        CollectStage(Collector<I, Object, O> collector) {
            this.collector = collector;
        }

        @Override
        public CompletionStage<O> apply(Multi<I> source) {
            return source.collectItems().with(collector).subscribeAsCompletionStage();
        }
    }

}
