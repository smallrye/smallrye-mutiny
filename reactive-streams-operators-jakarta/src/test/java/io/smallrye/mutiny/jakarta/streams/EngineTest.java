package io.smallrye.mutiny.jakarta.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.eclipse.microprofile.reactive.streams.operators.spi.UnsupportedStageException;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

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

    @Test
    public void testUnknownTerminalStage() {
        assertThrows(UnsupportedStageException.class, () -> {
            engine = new Engine();
            List<Stage> stages = new ArrayList<>();
            stages.add((Stage.Map) () -> i -> (int) i + 1);
            stages.add(new Stage() {
                // Unknown stage
            });
            Graph graph = () -> stages;
            engine.buildSubscriber(graph);
        });
    }

    @Test
    public void testInvalidSubscriber() {
        assertThrows(IllegalArgumentException.class, () -> {
            engine = new Engine();
            List<Stage> stages = new ArrayList<>();
            stages.add((Stage.Map) () -> i -> (int) i + 1);
            // This graph is not closed - so it's invalid
            Graph graph = () -> stages;
            engine.buildSubscriber(graph);
        });
    }

    @Test
    public void testValidCompletion() {
        engine = new Engine();
        List<Stage> stages = new ArrayList<>();
        stages.add((Stage.PublisherStage) () -> AdaptersToReactiveStreams.publisher(Multi.createFrom().empty()));
        stages.add((Stage.Map) () -> i -> (int) i + 1);
        stages.add(new Stage.FindFirst() {
        });
        Graph graph = () -> stages;
        assertThat(engine.buildCompletion(graph)).isNotNull();
    }

    @Test
    public void testInvalidCompletion() {
        assertThrows(IllegalArgumentException.class, () -> {
            engine = new Engine();
            List<Stage> stages = new ArrayList<>();
            stages.add((Stage.PublisherStage) () -> AdaptersToReactiveStreams.publisher(Multi.createFrom().empty()));
            stages.add((Stage.Map) () -> i -> (int) i + 1);
            // This graph is not closed - so it's invalid
            Graph graph = () -> stages;
            engine.buildCompletion(graph);
        });
    }

    @Test
    public void testCompletionWithUnknownStage() {
        assertThrows(UnsupportedStageException.class, () -> {
            engine = new Engine();
            List<Stage> stages = new ArrayList<>();
            stages.add((Stage.PublisherStage) () -> AdaptersToReactiveStreams.publisher(Multi.createFrom().empty()));
            stages.add((Stage.Map) () -> i -> (int) i + 1);
            stages.add(new Stage() {
                // Unknown stage.
            });
            stages.add(new Stage.FindFirst() {
            });
            Graph graph = () -> stages;
            assertThat(engine.buildCompletion(graph)).isNotNull();
        });
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

    @Test
    public void testInvalidPublisher() {
        assertThrows(IllegalArgumentException.class, () -> {
            engine = new Engine();
            List<Stage> stages = new ArrayList<>();
            stages.add((Stage.Map) () -> i -> (int) i + 1);
            stages.add(new Stage.FindFirst() {
            });
            // This graph is closed, invalid as publisher
            Graph graph = () -> stages;
            engine.buildPublisher(graph);
        });
    }

    @Test
    public void testCreatingPublisherWithUnknownStage() {
        assertThrows(UnsupportedStageException.class, () -> {
            engine = new Engine();
            List<Stage> stages = new ArrayList<>();
            stages.add(new Stage() {
                // Unknown stage.
            });
            stages.add((Stage.Map) () -> i -> (int) i + 1);
            Graph graph = () -> stages;
            engine.buildPublisher(graph);
        });
    }

}
