package io.smallrye.mutiny.tcktests;

import static io.smallrye.mutiny.tcktests.Await.await;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiCollectTest extends AbstractTck {

    @Test
    public void toListStageShouldReturnAList() {
        List<Integer> list = await(
                Multi.createFrom().items(1, 2, 3).collect().asList().subscribeAsCompletionStage());
        assertEquals(list, Arrays.asList(1, 2, 3));
    }

    @Test
    public void toListStageShouldReturnEmpty() {
        assertEquals(
                await(Multi.createFrom().items().collect().asList().subscribeAsCompletionStage()),
                Collections.emptyList());
    }

    @Test
    public void collectShouldAccumulateResult() {
        CompletableFuture<AtomicInteger> future = Multi.createFrom().items(1, 2, 3)
                .collect().in(
                        () -> new AtomicInteger(0),
                        AtomicInteger::addAndGet)
                .subscribeAsCompletionStage();
        assertEquals(await(future).get(), 6);
    }

    @Test
    public void collectShouldSupportEmptyStreams() {
        CompletableFuture<AtomicInteger> future = Multi.createFrom().<Integer> empty().collect().in(
                () -> new AtomicInteger(42),
                AtomicInteger::addAndGet)
                .subscribeAsCompletionStage();
        assertEquals(await(future).get(), 42);
    }

    @Test
    public void collectShouldPropagateUpstreamErrors() {
        Assert.assertThrows(QuietRuntimeException.class, () -> await(Multi.createFrom()
                .<Integer> failure(new QuietRuntimeException("failed"))
                .collect().in(
                        () -> new AtomicInteger(0),
                        AtomicInteger::addAndGet)
                .subscribeAsCompletionStage()));
    }

    @Test
    public void finisherFunctionShouldBeInvoked() {
        assertEquals(await(Multi.createFrom().items("1", "2", "3")
                .collect().with(Collectors.joining(", ")).subscribeAsCompletionStage()), "1, 2, 3");
    }

    @Test
    public void toListStageShouldPropagateUpstreamErrors() {
        Assert.assertThrows(QuietRuntimeException.class, () -> await(Multi.createFrom()
                .failure(new QuietRuntimeException("failed"))
                .collect().asList()
                .subscribeAsCompletionStage()));
    }

    @Test
    public void collectStageShouldPropagateErrorsFromSupplierThroughCompletionStage() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<Integer> result = null;
            try {
                result = infiniteStream()
                        .onTermination().invoke((failed, cancel) -> {
                            if (cancel) {
                                cancelled.complete(null);
                            }
                        })
                        .collect().with(Collector.of(() -> {
                            throw new QuietRuntimeException("failed");
                        }, (a, b) -> {
                        }, Integer::sum, Function.identity()))
                        .subscribeAsCompletionStage();
            } catch (Exception e) {
                Assert.fail(
                        "Exception thrown directly from stream, it should have been captured by the returned CompletionStage",
                        e);
            }
            await(cancelled);
            await(result);
        });
    }

    @Test
    public void collectStageShouldPropagateErrorsFromAccumulator() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();

            CompletionStage<String> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .collect().with(Collector.of(() -> "", (a, b) -> {
                        throw new QuietRuntimeException("failed");
                    }, (a, b) -> a + b, Function.identity()))
                    .subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Test
    public void collectStageShouldPropagateErrorsFromFinisher() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletionStage<Integer> result = Multi.createFrom().items(1, 2, 3)
                    .collect().with(Collector.<Integer, Integer, Integer> of(() -> 0, (a, b) -> {
                    },
                            Integer::sum,
                            r -> {
                                throw new QuietRuntimeException("failed");
                            }))
                    .subscribeAsCompletionStage();
            await(result);
        });
    }

}
