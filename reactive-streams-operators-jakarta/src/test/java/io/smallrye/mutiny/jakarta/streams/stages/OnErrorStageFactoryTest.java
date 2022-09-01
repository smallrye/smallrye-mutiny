package io.smallrye.mutiny.jakarta.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Checks the behavior of the {@link OnErrorStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class OnErrorStageFactoryTest extends StageTestBase {

    private final OnErrorStageFactory factory = new OnErrorStageFactory();

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        AtomicReference<Throwable> error = new AtomicReference<>();
        ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(publisher))
                .filter(i -> i < 4)
                .map(this::squareOrFailed)
                .onError(error::set)
                .map(this::asString)
                .toList()
                .run().toCompletableFuture().exceptionally(x -> Collections.emptyList()).get();
        assertThat(error.get()).hasMessage("failed");
    }

    private Integer squareOrFailed(int i) {
        if (i == 2) {
            throw new RuntimeException("failed");
        }
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
