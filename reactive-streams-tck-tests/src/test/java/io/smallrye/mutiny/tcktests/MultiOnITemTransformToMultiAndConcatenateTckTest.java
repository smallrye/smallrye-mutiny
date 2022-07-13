package io.smallrye.mutiny.tcktests;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiOnITemTransformToMultiAndConcatenateTckTest extends AbstractPublisherTck<Long> {

    private ScheduledExecutorService executor;

    @BeforeTest
    public void init() {
        executor = Executors.newScheduledThreadPool(4);
    }

    @AfterTest
    public void shutdown() {
        executor.shutdown();
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    @Test
    public void flatMapStageShouldMapElements() {
        Assert.assertEquals(Await.await(Multi.createFrom().items(1, 2, 3)
                .emitOn(executor)
                .onItem().transformToMultiAndConcatenate(n -> Multi.createFrom().items(n, n, n))
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 1, 1, 2, 2, 2, 3, 3, 3));
    }

    @Test
    public void flatMapStageShouldAllowEmptySubStreams() {
        Assert.assertEquals(Await.await(Multi.createFrom().items(Multi.createFrom().empty(), Multi.createFrom().items(1, 2))
                .onItem().transformToMultiAndConcatenate(Function.identity())
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 2));
    }

    @Test
    public void flatMapStageShouldHandleExceptions() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
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
                    .collect().asList()
                    .subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }

    @Test
    public void flatMapStageShouldPropagateUpstreamExceptions() {
        Assert.assertThrows(QuietRuntimeException.class,
                () -> Await.await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                        .onItem().transformToMultiAndConcatenate(x -> Multi.createFrom().item(x))
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void flatMapStageShouldPropagateSubstreamExceptions() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .onItem()
                    .transformToMultiAndConcatenate(
                            f -> Multi.createFrom().failure(new QuietRuntimeException("failed")))
                    .collect().asList()
                    .subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }

    @Test
    public void flatMapStageShouldPropagateCancelToSubstreams() {
        CompletableFuture<Void> outerCancelled = new CompletableFuture<>();
        CompletableFuture<Void> innerCancelled = new CompletableFuture<>();
        Await.await(infiniteStream()
                .onTermination().invoke(() -> outerCancelled.complete(null))
                .onItem()
                .transformToMultiAndConcatenate(
                        i -> infiniteStream().onTermination().invoke(() -> innerCancelled.complete(null)))
                .select().first(5)
                .collect().asList()
                .subscribeAsCompletionStage());

        Await.await(outerCancelled);
        Await.await(innerCancelled);
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onItem().transformToMultiAndConcatenate(x -> Multi.createFrom().item(x));
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onItem().transformToMultiAndConcatenate(x -> Multi.createFrom().item(x));
    }
}
