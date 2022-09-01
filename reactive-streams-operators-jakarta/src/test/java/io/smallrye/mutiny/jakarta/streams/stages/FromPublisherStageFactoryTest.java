package io.smallrye.mutiny.jakarta.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Checks the behavior of the {@link FromPublisherStageFactory} class.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FromPublisherStageFactoryTest extends StageTestBase {

    private final FromPublisherStageFactory factory = new FromPublisherStageFactory();

    @Test
    public void create() throws ExecutionException, InterruptedException {
        List<Integer> list = ReactiveStreams
                .fromPublisher(AdaptersToReactiveStreams.publisher(Multi.createFrom().items(1, 2, 3))).toList().run()
                .toCompletableFuture()
                .get();
        assertThat(list).containsExactly(1, 2, 3);

        Optional<Integer> res = ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(Multi.createFrom().item(25)))
                .findFirst().run()
                .toCompletableFuture()
                .get();
        assertThat(res).contains(25);

        Optional<?> empty = ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(Multi.createFrom().empty()))
                .findFirst().run()
                .toCompletableFuture()
                .get();
        assertThat(empty).isEmpty();

        try {
            ReactiveStreams
                    .fromPublisher(AdaptersToReactiveStreams.publisher(Multi.createFrom().failure(new Exception("Boom"))))
                    .findFirst().run()
                    .toCompletableFuture().get();
            fail("Exception should be thrown");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("Boom");
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void createWithoutStage() {
        assertThrows(NullPointerException.class, () -> factory.create(null, null));
    }

    @Test
    public void createWithoutFunction() {
        assertThrows(NullPointerException.class, () -> factory.create(null, () -> null));
    }
}
