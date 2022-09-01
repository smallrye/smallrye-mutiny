package io.smallrye.mutiny.jakarta.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Checks the behavior of the {@link OnCompleteStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class OnCompleteStageFactoryTest extends StageTestBase {

    private final OnCompleteStageFactory factory = new OnCompleteStageFactory();

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        AtomicBoolean completed = new AtomicBoolean();
        ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(publisher))
                .filter(i -> i < 4)
                .map(this::square)
                .onComplete(() -> completed.set(true))
                .map(this::asString)
                .toList()
                .run().toCompletableFuture().get();
        assertThat(completed).isTrue();
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
