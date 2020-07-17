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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.After;
import org.junit.Test;

import io.smallrye.mutiny.Multi;

/**
 * Checks the behavior of the {@link OnTerminateStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class OnTerminateStageFactoryTest extends StageTestBase {

    private final OnTerminateStageFactory factory = new OnTerminateStageFactory();

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @After
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void createWithFailure() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        AtomicBoolean error = new AtomicBoolean();
        ReactiveStreams.fromPublisher(publisher)
                .filter(i -> i < 4)
                .map(this::squareOrFailed)
                .onTerminate(() -> error.set(true))
                .map(this::asString)
                .toList()
                .run().toCompletableFuture().exceptionally(x -> Collections.emptyList()).get();
        await().untilAtomic(error, is(true));
        assertThat(error).isTrue();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        AtomicBoolean completed = new AtomicBoolean();
        ReactiveStreams.fromPublisher(publisher)
                .filter(i -> i < 4)
                .map(this::square)
                .onTerminate(() -> completed.set(true))
                .map(this::asString)
                .toList()
                .run().toCompletableFuture().get();
        await().untilAtomic(completed, is(true));
        assertThat(completed).isTrue();
    }

    private Integer squareOrFailed(int i) {
        if (i == 2) {
            throw new IllegalStateException("failed");
        }
        return i * i;
    }

    private Integer square(int i) {
        return i * i;
    }

    private String asString(int i) {
        return Objects.toString(i);
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
