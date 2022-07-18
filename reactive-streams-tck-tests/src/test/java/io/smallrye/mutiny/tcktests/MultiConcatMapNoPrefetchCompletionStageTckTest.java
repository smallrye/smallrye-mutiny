package io.smallrye.mutiny.tcktests;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiConcatMapNoPrefetchCompletionStageTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void flatMapCsStageShouldMapFutures() {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();
        CompletionStage<List<Integer>> result = Multi.createFrom().<CompletionStage<Integer>> items(one, two, three)
                .onItem().transformToUni(i -> Uni.createFrom().completionStage(i))
                .concatenate()
                .collect().asList()
                .subscribeAsCompletionStage();
        one.complete(1);
        two.complete(2);
        three.complete(3);
        Assert.assertEquals(Await.await(result), Arrays.asList(1, 2, 3));
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                // Use supplyAsync on purpose to check the concurrency.
                .onItem().transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .concatenate();
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                // Use supplyAsync on purpose to check the concurrency.
                .onItem().transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .concatenate();
    }

    @Test
    public void flatMapCsStageShouldMaintainOrderOfFuturesIfConcurrencyIs1() throws Exception {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();
        CompletionStage<List<Integer>> result = Multi.createFrom().<CompletionStage<Integer>> items(one, two, three)
                .onItem().transformToUni(i -> Uni.createFrom().completionStage(i)).concatenate()
                .collect().asList()
                .subscribeAsCompletionStage();
        three.complete(3);
        two.complete(2);
        one.complete(1);
        Assert.assertEquals(Await.await(result), Arrays.asList(1, 2, 3));
    }

    @Test
    public void flatMapCsStageShouldOnlyMapOneElementAtATimeWithConcurrencyIs1() throws Exception {
        CompletableFuture<Integer> one = new CompletableFuture<>();
        CompletableFuture<Integer> two = new CompletableFuture<>();
        CompletableFuture<Integer> three = new CompletableFuture<>();
        AtomicInteger concurrentMaps = new AtomicInteger(0);
        CompletionStage<List<Integer>> result = Multi.createFrom().<CompletionStage<Integer>> items(one, two, three)
                .onItem().transformToUni(cs -> {
                    Assert.assertEquals(1, concurrentMaps.incrementAndGet());
                    return Uni.createFrom().completionStage(cs);
                }).concatenate()
                .collect().asList()
                .subscribeAsCompletionStage();
        concurrentMaps.decrementAndGet();
        one.complete(1);
        concurrentMaps.decrementAndGet();
        two.complete(2);
        concurrentMaps.decrementAndGet();
        three.complete(3);
        Assert.assertEquals(Await.await(result), Arrays.asList(1, 2, 3));
    }

    @Test
    public void flatMapCsStageShouldPropagateUpstreamErrors() {
        Assert.assertThrows(QuietRuntimeException.class, () -> Await.await(
                Multi.createFrom().failure(new QuietRuntimeException("failed"))
                        .onItem().transformToUni(
                                i -> Uni.createFrom().completionStage(CompletableFuture.completedFuture(i)))
                        .concatenate()
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void flatMapCsStageShouldHandleErrorsThrownByCallback() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = this.infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .onItem().transformToUni(i -> {
                        throw new QuietRuntimeException("failed");
                    }).concatenate()
                    .collect().asList()
                    .subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }

    @Test
    public void flatMapCsStageShouldHandleFailedCompletionStages() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = this.infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .onItem().transformToUni(i -> {
                        CompletableFuture<Object> failed = new CompletableFuture<>();
                        failed.completeExceptionally(new QuietRuntimeException("failed"));
                        return Uni.createFrom().completionStage(failed);
                    }).concatenate()
                    .collect().asList()
                    .subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }

    @Test
    public void flatMapCsStageShouldFailIfNullIsReturned() {
        Assert.assertThrows(NullPointerException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = this.infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .onItem()
                    .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.completedFuture(null))
                            .onItem().ifNull().failWith(new NullPointerException()))
                    .concatenate()
                    .collect().asList()
                    .subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }
}
