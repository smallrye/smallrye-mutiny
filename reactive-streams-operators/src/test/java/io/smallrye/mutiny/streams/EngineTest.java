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
package io.smallrye.mutiny.streams;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.eclipse.microprofile.reactive.streams.operators.spi.UnsupportedStageException;
import org.junit.Test;

import io.smallrye.mutiny.Multi;

/**
 * Checks the behavior of the {@link Engine} class.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class EngineTest {

    private Engine engine;

    @Test
    public void create() {
        engine = new Engine();
        assertThat(engine).isNotNull();
    }

    @Test
    public void testValidSubscriber() {
        engine = new Engine();
        CompletionSubscriber<Integer, Optional<Integer>> built = ReactiveStreams.<Integer> builder()
                .map(i -> i + 1)
                .findFirst()
                .build(engine);

        assertThat(built).isNotNull();

        ReactiveStreams.of(5, 4, 3).buildRs().subscribe(built);
        Optional<Integer> integer = built.getCompletion().toCompletableFuture().join();
        assertThat(integer).contains(6);
    }

    @Test(expected = UnsupportedStageException.class)
    public void testUnknownTerminalStage() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        stages.add(new Stage() {
            // Unknown stage
        });
        Graph graph = () -> stages;
        engine.buildSubscriber(graph);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSubscriber() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        // This graph is not closed - so it's invalid
        Graph graph = () -> stages;
        engine.buildSubscriber(graph);
    }

    @Test
    public void testValidCompletion() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add((Stage.PublisherStage) () -> Multi.createFrom().empty());
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        stages.add(new Stage.FindFirst() {
        });
        Graph graph = () -> stages;
        assertThat(engine.buildCompletion(graph)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCompletion() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add((Stage.PublisherStage) () -> Multi.createFrom().empty());
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        // This graph is not closed - so it's invalid
        Graph graph = () -> stages;
        engine.buildCompletion(graph);
    }

    @Test(expected = UnsupportedStageException.class)
    public void testCompletionWithUnknownStage() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add((Stage.PublisherStage) () -> Multi.createFrom().empty());
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        stages.add(new Stage() {
            // Unknown stage.
        });
        stages.add(new Stage.FindFirst() {
        });
        Graph graph = () -> stages;
        assertThat(engine.buildCompletion(graph)).isNotNull();
    }

    @Test
    public void testValidPublisher() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add((Stage.Of) () -> Arrays.asList(1, 2, 3));
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        Graph graph = () -> stages;
        assertThat(engine.buildPublisher(graph)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPublisher() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        stages.add(new Stage.FindFirst() {
        });
        // This graph is closed, invalid as publisher
        Graph graph = () -> stages;
        engine.buildPublisher(graph);
    }

    @Test(expected = UnsupportedStageException.class)
    public void testCreatingPublisherWithUnknownStage() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add(new Stage() {
            // Unknown stage.
        });
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        Graph graph = () -> stages;
        engine.buildPublisher(graph);
    }

}
