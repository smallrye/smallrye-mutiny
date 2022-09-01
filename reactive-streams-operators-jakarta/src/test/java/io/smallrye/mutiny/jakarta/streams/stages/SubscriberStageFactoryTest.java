package io.smallrye.mutiny.jakarta.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Checks the behavior of the {@link SubscriberStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class SubscriberStageFactoryTest extends StageTestBase {

    private final SubscriberStageFactory factory = new SubscriberStageFactory();

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    public void create() {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor);

        SubscriberBuilder<Integer, Optional<Integer>> builder = ReactiveStreams.<Integer> builder().findFirst();

        Optional<Integer> optional = ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(publisher))
                .filter(i -> i > 5)
                .to(builder).run().toCompletableFuture().join();

        assertThat(optional).contains(6);
    }

    @Test
    public void createWithoutStage() {
        assertThrows(NullPointerException.class, () -> factory.create(null, null));
    }

    @Test
    public void createWithoutSubscriber() {
        assertThrows(NullPointerException.class, () -> factory.create(new Engine(), () -> null));
    }

}
