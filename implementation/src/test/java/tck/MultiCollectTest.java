package tck;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiCollectTest extends AbstractTck {

    @Test
    public void toListStageShouldReturnAList() {
        List<Integer> list = await(Multi.createFrom().items(1, 2, 3).collectItems().asList().subscribeAsCompletionStage());
        assertEquals(list, Arrays.asList(1, 2, 3));
    }

    @Test
    public void toListStageShouldReturnEmpty() {
        assertEquals(
                await(Multi.createFrom().items().collectItems().asList().subscribeAsCompletionStage()),
                Collections.emptyList());
    }

    @Test
    public void collectShouldAccumulateResult() {
        CompletableFuture<AtomicInteger> future = Multi.createFrom().items(1, 2, 3)
                .collectItems().in(
                        () -> new AtomicInteger(0),
                        AtomicInteger::addAndGet)
                .subscribeAsCompletionStage();
        assertEquals(await(future).get(), 6);
    }

    @Test
    public void collectShouldSupportEmptyStreams() {
        CompletableFuture<AtomicInteger> future = Multi.createFrom().<Integer> empty().collectItems().in(
                () -> new AtomicInteger(42),
                AtomicInteger::addAndGet)
                .subscribeAsCompletionStage();
        assertEquals(await(future).get(), 42);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void collectShouldPropagateUpstreamErrors() {
        await(Multi.createFrom()
                .<Integer> failure(new QuietRuntimeException("failed"))
                .collectItems().in(
                        () -> new AtomicInteger(0),
                        AtomicInteger::addAndGet)
                .subscribeAsCompletionStage());
    }

    @Test
    public void finisherFunctionShouldBeInvoked() {
        assertEquals(await(Multi.createFrom().items("1", "2", "3")
                .collectItems().with(Collectors.joining(", ")).subscribeAsCompletionStage()), "1, 2, 3");
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void toListStageShouldPropagateUpstreamErrors() {
        await(Multi.createFrom()
                .failure(new QuietRuntimeException("failed"))
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void collectStageShouldPropagateErrorsFromSupplierThroughCompletionStage() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<Integer> result = null;
        try {
            result = infiniteStream()
                    .onTermination().invoke((failed, cancel) -> {
                        if (cancel) {
                            cancelled.complete(null);
                        }
                    })
                    .collectItems().with(Collector.<Integer, Integer, Integer> of(() -> {
                        throw new QuietRuntimeException("failed");
                    }, (a, b) -> {
                    }, (a, b) -> a + b, Function.identity()))
                    .subscribeAsCompletionStage();
        } catch (Exception e) {
            fail("Exception thrown directly from stream, it should have been captured by the returned CompletionStage",
                    e);
        }
        await(cancelled);
        await(result);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void collectStageShouldPropagateErrorsFromAccumulator() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<String> result = infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .collectItems().with(Collector.of(() -> "", (a, b) -> {
                    throw new QuietRuntimeException("failed");
                }, (a, b) -> a + b, Function.identity()))
                .subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void collectStageShouldPropagateErrorsFromFinisher() {
        CompletionStage<Integer> result = Multi.createFrom().items(1, 2, 3)
                .collectItems().with(Collector.<Integer, Integer, Integer> of(() -> 0, (a, b) -> {
                },
                        (a, b) -> a + b,
                        r -> {
                            throw new QuietRuntimeException("failed");
                        }))
                .subscribeAsCompletionStage();
        await(result);
    }

}
