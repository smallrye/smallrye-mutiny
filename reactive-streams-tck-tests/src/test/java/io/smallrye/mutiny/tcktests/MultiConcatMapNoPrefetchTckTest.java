package io.smallrye.mutiny.tcktests;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiConcatMapNoPrefetchTckTest extends AbstractPublisherTck<Long> {

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
                .onItem().transformToMulti(n -> Multi.createFrom().items(n, n, n))
                .concatenate()
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 1, 1, 2, 2, 2, 3, 3, 3));
    }

    @Test
    public void flatMapStageShouldAllowEmptySubStreams() {
        Assert.assertEquals(Await.await(Multi.createFrom().items(Multi.createFrom().empty(), Multi.createFrom().items(1, 2))
                .onItem().transformToMulti(Function.identity())
                .concatenate()
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
                    .onItem().transformToMulti(foo -> {
                        throw new QuietRuntimeException("failed");
                    })
                    .concatenate()
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
                        .onItem().transformToMulti(x -> Multi.createFrom().item(x))
                        .concatenate()
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void flatMapStageShouldPropagateSubstreamExceptions() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .onItem().transformToMulti(f -> Multi.createFrom().failure(new QuietRuntimeException("failed")))
                    .concatenate()
                    .collect().asList()
                    .subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }

    @Test
    public void concatMapStageShouldOnlySubscribeToOnePublisherAtATime() throws Exception {
        AtomicInteger activePublishers = new AtomicInteger();

        CompletionStage<List<Integer>> result = Multi.createFrom().items(1, 2, 3, 4, 5)
                .onItem().transformToMulti(id -> Multi.createFrom()
                        .publisher(new ScheduledPublisher(id, activePublishers, this::getExecutor)))
                .concatenate()
                .collect().asList()
                .subscribeAsCompletionStage();

        Assert.assertEquals(result.toCompletableFuture().get(2, TimeUnit.SECONDS),
                Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void flatMapStageShouldPropagateCancelToSubstreams() {
        CompletableFuture<Void> outerCancelled = new CompletableFuture<>();
        CompletableFuture<Void> innerCancelled = new CompletableFuture<>();
        Await.await(infiniteStream()
                .onTermination().invoke(() -> outerCancelled.complete(null))
                .onItem().transformToMulti(i -> infiniteStream().onTermination().invoke(() -> innerCancelled.complete(null)))
                .concatenate()
                .select().first(5)
                .collect().asList()
                .subscribeAsCompletionStage());

        Await.await(outerCancelled);
        Await.await(innerCancelled);
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .concatMap(x -> Multi.createFrom().item(x));
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .concatMap(x -> Multi.createFrom().item(x));
    }
}
