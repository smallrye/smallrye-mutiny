package tck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tck.Await.await;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.LongStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiOnITemTransformToMultiAndConcatenateTckTest extends AbstractPublisherTck<Long> {

    private ScheduledExecutorService executor;

    @BeforeEach
    public void init() {
        executor = Executors.newScheduledThreadPool(4);
    }

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    @Test
    public void flatMapStageShouldMapElements() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3)
                .emitOn(executor)
                .onItem().transformToMultiAndConcatenate(n -> Multi.createFrom().items(n, n, n))
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 1, 1, 2, 2, 2, 3, 3, 3));
    }

    @Test
    public void flatMapStageShouldAllowEmptySubStreams() {
        assertEquals(await(Multi.createFrom().items(Multi.createFrom().empty(), Multi.createFrom().items(1, 2))
                .onItem().transformToMultiAndConcatenate(Function.identity())
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 2));
    }

    @Test
    public void flatMapStageShouldHandleExceptions() {
        assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = infiniteStream()
                    .onTermination().invoke((f, c) -> {
                        if (c) {
                            cancelled.complete(null);
                        }
                    })
                    .onItem().transformToMultiAndConcatenate(foo -> {
                        throw new QuietRuntimeException("failed");
                    })
                    .collectItems().asList()
                    .subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Test
    public void flatMapStageShouldPropagateUpstreamExceptions() {
        assertThrows(QuietRuntimeException.class, () -> await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                .onItem().transformToMultiAndConcatenate(x -> Multi.createFrom().item(x))
                .collectItems().asList()
                .subscribeAsCompletionStage()));
    }

    @Test
    public void flatMapStageShouldPropagateSubstreamExceptions() {
        assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .onItem()
                    .transformToMultiAndConcatenate(
                            f -> Multi.createFrom().failure(new QuietRuntimeException("failed")))
                    .collectItems().asList()
                    .subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Test
    public void flatMapStageShouldPropagateCancelToSubstreams() {
        CompletableFuture<Void> outerCancelled = new CompletableFuture<>();
        CompletableFuture<Void> innerCancelled = new CompletableFuture<>();
        await(infiniteStream()
                .onTermination().invoke(() -> outerCancelled.complete(null))
                .onItem()
                .transformToMultiAndConcatenate(
                        i -> infiniteStream().onTermination().invoke(() -> innerCancelled.complete(null)))
                .transform().byTakingFirstItems(5)
                .collectItems().asList()
                .subscribeAsCompletionStage());

        await(outerCancelled);
        await(innerCancelled);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().items(LongStream.rangeClosed(1, elements).boxed())
                .onItem().transformToMultiAndConcatenate(x -> Multi.createFrom().item(x));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onItem().transformToMultiAndConcatenate(x -> Multi.createFrom().item(x));
    }
}
