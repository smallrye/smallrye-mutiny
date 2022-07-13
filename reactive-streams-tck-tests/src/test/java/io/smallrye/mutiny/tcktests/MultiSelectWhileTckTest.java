package io.smallrye.mutiny.tcktests;

import static io.smallrye.mutiny.tcktests.Await.await;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Publisher;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiSelectWhileTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void takeWhileStageShouldTakeWhileConditionIsTrue() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3, 4, 5, 6, 1, 2)
                .select().first(i -> i < 5)
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 2, 3, 4));
    }

    @Test
    public void takeWhileStageShouldEmitEmpty() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3, 4, 5, 6)
                .select().first(i -> false)
                .collect().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void takeWhileShouldCancelUpStreamWhenDone() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .select().first(t -> false)
                .collect().asList()
                .subscribeAsCompletionStage();
        await(cancelled);
    }

    @Test
    public void takeWhileShouldIgnoreSubsequentErrorsWhenDone() {
        assertEquals(await(
                Multi.createFrom().items(1, 2, 3, 4)
                        .flatMap(i -> {
                            if (i == 4) {
                                return Multi.createFrom().failure(new QuietRuntimeException("failed"));
                            } else {
                                return Multi.createFrom().items(i);
                            }
                        })
                        .select().first(t -> t < 3)
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2));
    }

    @Test
    public void takeWhileStageShouldHandleErrors() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Integer>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .select().first(i -> {
                        throw new QuietRuntimeException("failed");
                    })
                    .collect().asList()
                    .subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .select().first(x -> true);
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .select().first(x -> true);
    }
}
