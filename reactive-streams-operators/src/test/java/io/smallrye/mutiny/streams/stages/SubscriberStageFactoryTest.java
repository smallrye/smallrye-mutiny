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

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.junit.After;
import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;

/**
 * Checks the behavior of the {@link SubscriberStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class SubscriberStageFactoryTest extends StageTestBase {

    private final SubscriberStageFactory factory = new SubscriberStageFactory();

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @After
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    public void create() {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        SubscriberBuilder<Integer, Optional<Integer>> builder = ReactiveStreams.<Integer> builder().findFirst();

        Optional<Integer> optional = ReactiveStreams.fromPublisher(publisher).filter(i -> i > 5)
                .to(builder).run().toCompletableFuture().join();

        assertThat(optional).contains(6);
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutStage() {
        factory.create(new Engine(), null);
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutSubscriber() {
        factory.create(new Engine(), () -> null);
    }

}
