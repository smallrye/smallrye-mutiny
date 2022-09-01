package io.smallrye.mutiny.jakarta.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.tck.spi.QuietRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Checks the behavior of the {@link DropWhileStageFactory} class.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class DropWhileStageFactoryTest extends StageTestBase {

    private final DropWhileStageFactory factory = new DropWhileStageFactory();

    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        List<Integer> list = ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).dropWhile(i -> i < 6)
                .toList().run()
                .toCompletableFuture().get();
        assertThat(list).hasSize(5).containsExactly(6, 7, 8, 9, 10);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void createWithoutStage() {
        assertThrows(NullPointerException.class, () -> factory.create(null, null));
    }

    @Test
    public void createWithoutPredicate() {
        assertThrows(NullPointerException.class, () -> factory.create(null, () -> null));
    }

    @Test
    public void dropWhileStageShouldPropagateUpstreamErrorsAfterFinishedDropping() {
        assertThrows(QuietRuntimeException.class, () -> this.awaitCompletion(this.infiniteStream().peek((i) -> {
            if (i == 4) {
                throw new QuietRuntimeException("failed");
            }
        }).dropWhile((i) -> i < 3).toList().run()));
    }

}
