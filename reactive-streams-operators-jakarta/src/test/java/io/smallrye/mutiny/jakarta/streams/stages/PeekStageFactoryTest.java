package io.smallrye.mutiny.jakarta.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Checks the behavior of the {@link PeekStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class PeekStageFactoryTest extends StageTestBase {

    private final PeekStageFactory factory = new PeekStageFactory();

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        List<Integer> squares = new ArrayList<>();
        List<String> strings = new ArrayList<>();
        List<String> list = ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(publisher))
                .filter(i -> i < 4)
                .map(this::square)
                .peek(squares::add)
                .map(this::asString)
                .peek(strings::add)
                .toList()
                .run().toCompletableFuture().get();

        assertThat(list).containsExactly("1", "4", "9");
        assertThat(squares).containsExactly(1, 4, 9);
        assertThat(strings).containsExactly("1", "4", "9");
    }

    private Integer square(int i) {
        return i * i;
    }

    private String asString(int i) {
        return Objects.toString(i);
    }

    @Test
    public void createWithoutStage() {
        assertThrows(NullPointerException.class, () -> factory.create(null, null));
    }

    @Test
    public void createWithoutFunction() {
        assertThrows(NullPointerException.class, () -> factory.create(null, () -> null));
    }

}
