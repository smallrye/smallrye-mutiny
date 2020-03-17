package io.smallrye.mutiny.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.After;
import org.junit.Test;

import io.smallrye.mutiny.Multi;

/**
 * Checks the behavior of the {@link FlatMapStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FlatMapStageFactoryTest extends StageTestBase {

    private final FlatMapStageFactory factory = new FlatMapStageFactory();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ExecutorService computation = Executors.newFixedThreadPool(100);

    @After
    public void cleanup() {
        computation.shutdown();
        executor.shutdown();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        //                .emitOn(computation);

        List<String> list = ReactiveStreams.fromPublisher(publisher)
                .filter(i -> i < 4)
                .flatMap(this::duplicate)
                .flatMapCompletionStage(this::asString)
                .toList()
                .run().toCompletableFuture().get();

        assertThat(list).containsExactly("1", "1", "2", "2", "3", "3");
    }

    private PublisherBuilder<Integer> duplicate(int i) {
        return ReactiveStreams.fromPublisher(Multi.createFrom().items(i, i));
        //.emitOn(computation))
    }

    private CompletionStage<String> asString(int i) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        executor.submit(() -> cf.complete(Objects.toString(i)));
        return cf;
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutStage() {
        factory.create(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutFunction() {
        factory.create(null, () -> null);
    }

}
