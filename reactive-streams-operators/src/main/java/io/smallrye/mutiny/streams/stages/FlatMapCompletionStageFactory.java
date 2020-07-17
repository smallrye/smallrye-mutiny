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
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.streams.utils.Casts;

/**
 * Implementation of the {@link Stage.FlatMapCompletionStage} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FlatMapCompletionStageFactory
        implements ProcessingStageFactory<Stage.FlatMapCompletionStage> {

    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine,
            Stage.FlatMapCompletionStage stage) {
        Function<I, CompletionStage<O>> mapper = Casts.cast(
                Objects.requireNonNull(stage).getMapper());
        return new FlatMapCompletionStage<>(mapper);
    }

    private static class FlatMapCompletionStage<I, O> implements ProcessingStage<I, O> {
        private final Function<I, CompletionStage<O>> mapper;

        private FlatMapCompletionStage(Function<I, CompletionStage<O>> mapper) {
            this.mapper = Objects.requireNonNull(mapper);
        }

        @Override
        public Multi<O> apply(Multi<I> source) {
            return source.onItem().transformToUni((I item) -> {
                if (item == null) {
                    // Propagate an NPE to be compliant with the reactive stream spec.
                    return Uni.createFrom().failure(new NullPointerException());
                }
                CompletionStage<O> result = mapper.apply(item);
                if (result == null) {
                    // Propagate an NPE to be compliant with the reactive stream spec.
                    return Uni.createFrom().failure(new NullPointerException());
                }
                return Uni.createFrom().completionStage(result)
                        .onItem().ifNull().failWith(new NullPointerException());
            }).concatenate();
        }
    }

}
