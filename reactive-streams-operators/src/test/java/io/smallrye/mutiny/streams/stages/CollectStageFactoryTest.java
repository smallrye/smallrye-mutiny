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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.tck.spi.QuietRuntimeException;
import org.junit.After;
import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.TerminalStage;

/**
 * Checks the behavior of the {@link CollectStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CollectStageFactoryTest extends StageTestBase {

    private final CollectStageFactory factory = new CollectStageFactory();

    private final ExecutorService computation = Executors.newFixedThreadPool(4);

    @After
    public void cleanup() {
        computation.shutdown();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        TerminalStage<Integer, Integer> terminal = factory.create(null,
                () -> Collectors.summingInt((ToIntFunction<Integer>) value -> value));

        List<Integer> list = new ArrayList<>();
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .onItem().invoke(list::add)
                .emitOn(computation);
        CompletionStage<Integer> stage = terminal.apply(publisher);
        Integer result = stage.toCompletableFuture().get();

        assertThat(result).isEqualTo(1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10);
        assertThat(list).hasSize(10);
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutStage() {
        factory.create(new Engine(), null);
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutCollector() {
        factory.create(null, () -> null);
    }

    @Test
    public void collectStageShouldPropagateErrorsFromSupplierThroughCompletionStage() {
        CompletionStage<Integer> result = infiniteStream()
                .collect(Collector.<Integer, Integer, Integer> of(() -> {
                    throw new QuietRuntimeException("failed");
                }, (a, b) -> {
                }, (a, b) -> a + b, Function.identity()))
                .run();
        await().until(() -> result.toCompletableFuture().isDone());
        try {
            result.toCompletableFuture().get();
            fail("Exception expected");
        } catch (Exception e) {
            if (!(e.getCause() instanceof QuietRuntimeException)) {
                fail("Quiet runtime Exception expected");
            }
        }

    }

    @Test(expected = QuietRuntimeException.class)
    public void collectStageShouldPropagateErrorsFromAccumulator() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<String> result = this.infiniteStream().onTerminate(() -> {
            cancelled.complete(null);
        }).collect(Collector.of(() -> "", (a, b) -> {
            throw new QuietRuntimeException("failed");
        }, (a, b) -> a + b, Function.identity())).run();
        this.awaitCompletion(cancelled);
        this.awaitCompletion(result);
    }

    @Test(expected = QuietRuntimeException.class)
    public void collectStageShouldPropagateErrorsFromFinisher() {
        CompletionStage<Object> result = ReactiveStreams.of(new Integer[] { 1, 2, 3 }).collect(Collector.of(
                () -> 0,
                (a, b) -> {
                },
                (a, b) -> a + b,
                (r) -> {
                    throw new QuietRuntimeException("failed");
                }))
                .run();
        this.awaitCompletion(result);
    }

    @Test(expected = QuietRuntimeException.class)
    public void collectStageShouldPropagateErrorsFromSupplierThroughCompletionStage2() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<Integer> result = null;
        try {
            result = infiniteStream()
                    .onTerminate(() -> cancelled.complete(null))
                    .collect(Collector.<Integer, Integer, Integer> of(() -> {
                        throw new QuietRuntimeException("failed");
                    }, (a, b) -> {
                    }, (a, b) -> a + b, Function.identity()))
                    .run();
        } catch (Exception e) {
            fail("Exception thrown directly from stream, it should have been captured by the returned CompletionStage",
                    e);
        }
        awaitCompletion(cancelled);
        awaitCompletion(result);
    }

}
