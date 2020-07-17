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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.TerminalStage;
import io.smallrye.mutiny.streams.operators.TerminalStageFactory;

/**
 * Implementation of the {@link Stage.FindFirst} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FindFirstStageFactory implements TerminalStageFactory<Stage.FindFirst> {

    private static final TerminalStage<?, Optional<?>> INSTANCE = source -> {
        CompletableFuture<Optional<?>> future = new CompletableFuture<>();
        source
                .collectItems().first()
                .subscribe().with(
                        item -> future.complete(Optional.ofNullable(item)),
                        future::completeExceptionally);
        return future;
    };

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> TerminalStage<I, O> create(Engine engine, Stage.FindFirst stage) {
        Objects.requireNonNull(stage); // Not really useful here as it conveys no parameters, so just here for symmetry
        return (TerminalStage<I, O>) INSTANCE;
    }

}
