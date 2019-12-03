package tck;

import static org.testng.Assert.assertEquals;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiTakeFirstItemsTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void limitStageShouldLimitTheOutputElements() {
        assertEquals(await(infiniteStream()
                .transform().byTakingFirstItems(3)
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 2, 3));
    }

    @Test
    public void limitStageShouldAllowLimitingToZero() {
        assertEquals(await(infiniteStream()
                .transform().byTakingFirstItems(0)
                .collectItems().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void limitStageToZeroShouldCompleteStreamEvenWhenNoElementsAreReceived() {
        assertEquals(await(Multi.createFrom().publisher(subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        })).transform().byTakingFirstItems(0)
                .collectItems().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void limitStageShouldCancelUpStreamWhenDone() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .on().termination((f, c) -> cancelled.complete(null))
                .transform().byTakingFirstItems(1)
                .collectItems().asList()
                .subscribeAsCompletionStage();
        await(cancelled);
    }

    @Test
    public void limitStageShouldIgnoreSubsequentErrorsWhenDone() {
        assertEquals(await(
                infiniteStream()
                        .flatMap(i -> {
                            if (i == 4) {
                                return Multi.createFrom().failure(new RuntimeException("failed"));
                            } else {
                                return Multi.createFrom().item(i);
                            }
                        })
                        .transform().byTakingFirstItems(3)
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void limitStageShouldPropagateCancellation() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        await(
                infiniteStream()
                        .on().termination((f, c) -> cancelled.complete(null))
                        .onItem().consume(i -> {
                            if (i == 100) {
                                cancelled.completeExceptionally(new RuntimeException("Was not cancelled"));
                            }
                        })
                        .transform().byTakingFirstItems(100)
                        .transform().byTakingFirstItems(3)
                        .collectItems().asList()
                        .subscribeAsCompletionStage());
        await(cancelled);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().items(LongStream.rangeClosed(1, elements).boxed())
                .transform().byTakingFirstItems(Long.MAX_VALUE);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .transform().byTakingFirstItems(Long.MAX_VALUE);
    }
}
