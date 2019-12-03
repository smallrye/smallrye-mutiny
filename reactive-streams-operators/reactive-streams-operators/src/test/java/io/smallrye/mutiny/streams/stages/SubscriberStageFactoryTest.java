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
                .subscribeOn(executor);

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
