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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.After;
import org.junit.Test;
import org.reactivestreams.Processor;

import io.smallrye.mutiny.Multi;

/**
 * Checks the behavior of the {@link ProcessorStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ProcessorStageFactoryTest extends StageTestBase {

    private final ProcessorStageFactory factory = new ProcessorStageFactory();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @After
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    public void createWithProcessors() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        List<String> list = ReactiveStreams.fromPublisher(publisher)
                .filter(i -> i < 4)
                .via(duplicateProcessor())
                .via(asStringProcessor())
                .toList()
                .run().toCompletableFuture().get();

        assertThat(list).containsExactly("1", "1", "2", "2", "3", "3");
    }

    @Test
    public void createWithProcessorBuilders() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        List<String> list = ReactiveStreams.fromPublisher(publisher)
                .filter(i -> i < 4)
                .via(duplicateProcessorBuilder())
                .via(asStringProcessorBuilder())
                .toList()
                .run().toCompletableFuture().get();

        assertThat(list).containsExactly("1", "1", "2", "2", "3", "3");
    }

    private ProcessorBuilder<Integer, Integer> duplicateProcessorBuilder() {
        return ReactiveStreams.<Integer> builder().flatMapIterable(i -> Arrays.asList(i, i));
    }

    private Processor<Integer, Integer> duplicateProcessor() {
        return duplicateProcessorBuilder().buildRs();
    }

    private ProcessorBuilder<Integer, String> asStringProcessorBuilder() {
        return ReactiveStreams.<Integer> builder().map(Object::toString);
    }

    private Processor<Integer, String> asStringProcessor() {
        return asStringProcessorBuilder().buildRs();
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutStage() {
        factory.create(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutFunction() {
        factory.create(null, () -> null);
    }

}
