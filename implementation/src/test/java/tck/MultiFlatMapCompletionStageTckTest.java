package tck;

import static org.assertj.core.api.Assertions.assertThat;
import static tck.Await.await;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.reactivestreams.Publisher;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiFlatMapCompletionStageTckTest extends AbstractPublisherTck<Integer> {

    @Test
    public void flatMapCsStageShouldMapFutures() {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();
        CompletionStage<List<Integer>> result = Multi.createFrom().<CompletionStage<Integer>> items(one, two, three)
                .onItem().applyCompletionStage(Function.identity()).merge()
                .collectItems().asList()
                .subscribeAsCompletionStage();
        one.complete(1);
        two.complete(2);
        three.complete(3);
        Assert.assertEquals(await(result), Arrays.asList(1, 2, 3));
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return Multi.createFrom().items(IntStream.rangeClosed(1, (int) elements).boxed())
                // Use supplyAsync on purpose to check the concurrency.
                .onItem().applyCompletionStage(i -> CompletableFuture.supplyAsync(() -> i)).merge();
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return Multi.createFrom().<Integer> failure(new RuntimeException("failed"))
                // Use supplyAsync on purpose to check the concurrency.
                .onItem().applyCompletionStage(i -> CompletableFuture.supplyAsync(() -> i)).merge();
    }

    @Test
    public void flatMapCsStageShouldMaintainOrderOfFuturesIfConcurrencyIs1() throws Exception {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();
        CompletionStage<List<Integer>> result = Multi.createFrom().<CompletionStage<Integer>> items(one, two, three)
                .onItem().applyCompletionStage(Function.identity()).merge(1)
                .collectItems().asList()
                .subscribeAsCompletionStage();
        three.complete(3);
        Thread.sleep(100L); //NOSONAR
        two.complete(2);
        Thread.sleep(100L); //NOSONAR
        one.complete(1);
        Assert.assertEquals(await(result), Arrays.asList(1, 2, 3));
    }

    @Test
    public void flatMapCsStageShouldOnlyMapOneElementAtATimeWithConcurrencyIs1() throws Exception {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();
        AtomicInteger concurrentMaps = new AtomicInteger(0);
        CompletionStage<List<Integer>> result = Multi.createFrom().<CompletionStage<Integer>> items(one, two, three)
                .onItem().applyCompletionStage(cs -> {
                    Assert.assertEquals(1, concurrentMaps.incrementAndGet());
                    return cs;
                }).merge(1)
                .collectItems().asList()
                .subscribeAsCompletionStage();
        Thread.sleep(100L); // NOSONAR
        concurrentMaps.decrementAndGet();
        one.complete(1);
        Thread.sleep(100L); // NOSONAR
        concurrentMaps.decrementAndGet();
        two.complete(2);
        Thread.sleep(100L); // NOSONAR
        concurrentMaps.decrementAndGet();
        three.complete(3);
        Assert.assertEquals(await(result), Arrays.asList(1, 2, 3));
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void flatMapCsStageShouldPropagateUpstreamErrors() {
        await(
                Multi.createFrom().failure(new QuietRuntimeException("failed"))
                        .onItem().applyCompletionStage(CompletableFuture::completedFuture).merge()
                        .collectItems().asList()
                        .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void flatMapCsStageShouldHandleErrorsThrownByCallback() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<List<Object>> result = this.infiniteStream()
                .on().termination(() -> cancelled.complete(null))
                .onItem().applyCompletionStage(i -> {
                    throw new QuietRuntimeException("failed");
                }).merge()
                .collectItems().asList()
                .subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void flatMapCsStageShouldHandleFailedCompletionStages() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<List<Object>> result = this.infiniteStream()
                .on().termination(() -> cancelled.complete(null))
                .onItem().applyCompletionStage(i -> {
                    CompletableFuture<Object> failed = new CompletableFuture<>();
                    failed.completeExceptionally(new QuietRuntimeException("failed"));
                    return failed;
                }).merge()
                .collectItems().asList()
                .subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Test
    public void flatMapCsStageShouldSkipIfNullIsReturned() {
        List<Object> result = Multi.createFrom().items(1, 2, 3)
                .onItem().applyCompletionStage(i -> CompletableFuture.completedFuture(null)).merge()
                .collectItems().asList()
                .await().indefinitely();
        assertThat(result).isEmpty();

    }
}
