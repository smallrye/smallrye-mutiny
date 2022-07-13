package io.smallrye.mutiny.tcktests;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscription;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiSelectFirstItemsTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void limitStageShouldLimitTheOutputElements() {
        Assert.assertEquals(Await.await(infiniteStream()
                .select().first(3)
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 2, 3));
    }

    @Test
    public void limitStageShouldAllowLimitingToZero() {
        Assert.assertEquals(Await.await(infiniteStream()
                .select().first(0)
                .collect().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void limitStageToZeroShouldCompleteStreamEvenWhenNoElementsAreReceived() {
        Assert.assertEquals(Await.await(Multi.createFrom().publisher(subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        }))
                .select().first(0)
                .collect().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void limitStageShouldCancelUpStreamWhenDone() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .select().first(1)
                .collect().asList()
                .subscribeAsCompletionStage();
        Await.await(cancelled);
    }

    @Test
    public void limitStageShouldIgnoreSubsequentErrorsWhenDone() {
        Assert.assertEquals(Await.await(
                infiniteStream()
                        .flatMap(i -> {
                            if (i == 4) {
                                return Multi.createFrom().failure(new RuntimeException("failed"));
                            } else {
                                return Multi.createFrom().item(i);
                            }
                        })
                        .select().first(3)
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void limitStageShouldPropagateCancellation() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        Await.await(
                infiniteStream()
                        .onTermination().invoke(() -> cancelled.complete(null))
                        .onItem().invoke(i -> {
                            if (i == 100) {
                                cancelled.completeExceptionally(new RuntimeException("Was not cancelled"));
                            }
                        })
                        .select().first(100)
                        .select().first(3)
                        .collect().asList()
                        .subscribeAsCompletionStage());
        Await.await(cancelled);
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .select().first(Long.MAX_VALUE);
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .select().first(Long.MAX_VALUE);
    }
}
