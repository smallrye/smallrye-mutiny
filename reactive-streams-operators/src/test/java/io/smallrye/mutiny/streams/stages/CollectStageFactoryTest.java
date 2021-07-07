package io.smallrye.mutiny.streams.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.tck.spi.QuietRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.TerminalStage;

/**
 * Checks the behavior of the {@link CollectStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CollectStageFactoryTest extends StageTestBase {

    private final CollectStageFactory factory = new CollectStageFactory();

    private final ExecutorService computation = Executors.newFixedThreadPool(4);

    @AfterEach
    public void cleanup() {
        computation.shutdown();
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        TerminalStage<Integer, Integer> terminal = factory.create(null,
                () -> Collectors.summingInt((ToIntFunction<Integer>) value -> value));

        List<Integer> list = new ArrayList<>();
        Multi<Integer> publisher = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .onItem().invoke(list::add)
                .emitOn(computation);
        CompletionStage<Integer> stage = terminal.apply(publisher);
        Integer result = stage.toCompletableFuture().get();

        assertThat(result).isEqualTo(1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10);
        assertThat(list).hasSize(10);
    }

    @Test
    public void createWithoutStage() {
        assertThrows(NullPointerException.class, () -> factory.create(new Engine(), null));
    }

    @Test
    public void createWithoutCollector() {
        assertThrows(NullPointerException.class, () -> factory.create(null, () -> null));
    }

    @Test
    public void collectStageShouldPropagateErrorsFromSupplierThroughCompletionStage() {
        CompletionStage<Integer> result = infiniteStream()
                .collect(Collector.<Integer, Integer, Integer> of(() -> {
                    throw new QuietRuntimeException("failed");
                }, (a, b) -> {
                }, (a, b) -> a + b, Function.identity()))
                .run();
        await().until(() -> result.toCompletableFuture().isDone());
        try {
            result.toCompletableFuture().get();
            fail("Exception expected");
        } catch (Exception e) {
            if (!(e.getCause() instanceof QuietRuntimeException)) {
                fail("Quiet runtime Exception expected");
            }
        }

    }

    @Test
    public void collectStageShouldPropagateErrorsFromAccumulator() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<String> result = this.infiniteStream().onTerminate(() -> {
            cancelled.complete(null);
        }).collect(Collector.of(() -> "", (a, b) -> {
            throw new QuietRuntimeException("failed");
        }, (a, b) -> a + b, Function.identity())).run();
        this.awaitCompletion(cancelled);
        assertThrows(QuietRuntimeException.class, () -> this.awaitCompletion(result));

    }

    @Test
    public void collectStageShouldPropagateErrorsFromFinisher() {
        CompletionStage<Object> result = ReactiveStreams.of(new Integer[] { 1, 2, 3 }).collect(Collector.of(
                () -> 0,
                (a, b) -> {
                },
                Integer::sum,
                (r) -> {
                    throw new QuietRuntimeException("failed");
                }))
                .run();
        assertThrows(QuietRuntimeException.class, () -> this.awaitCompletion(result));
    }

    @Test
    public void collectStageShouldPropagateErrorsFromSupplierThroughCompletionStage2() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<Integer> result = null;
        try {
            result = infiniteStream()
                    .onTerminate(() -> cancelled.complete(null))
                    .collect(Collector.<Integer, Integer, Integer> of(() -> {
                        throw new QuietRuntimeException("failed");
                    }, (a, b) -> {
                    }, Integer::sum, Function.identity()))
                    .run();
        } catch (Exception e) {
            fail("Exception thrown directly from stream, it should have been captured by the returned CompletionStage",
                    e);
        }
        awaitCompletion(cancelled);
        CompletionStage<Integer> actual = result;
        assertThrows(QuietRuntimeException.class, () -> this.awaitCompletion(actual));
    }

}
