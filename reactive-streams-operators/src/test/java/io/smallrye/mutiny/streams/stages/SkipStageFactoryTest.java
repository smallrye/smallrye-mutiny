package io.smallrye.mutiny.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Checks the behavior of the {@link SkipStageFactory} class.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class SkipStageFactoryTest extends StageTestBase {

    private final SkipStageFactory factory = new SkipStageFactory();

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);
        List<Integer> list = ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).skip(5).toList()
                .run()
                .toCompletableFuture().get();
        assertThat(list).hasSize(5).containsExactly(6, 7, 8, 9, 10);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void createWithoutStage() {
        assertThrows(NullPointerException.class, () -> factory.create(null, null));
    }

}
