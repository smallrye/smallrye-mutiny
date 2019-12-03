package tck;

import static org.testng.Assert.assertEquals;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiSkipItemsWhileTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void dropWhileStageShouldSupportDroppingElements() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3, 4, 0)
                .transform().bySkippingItemsWhile(i -> i < 3)
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList(3, 4, 0));
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void dropWhileStageShouldHandleErrors() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<List<Integer>> result = infiniteStream()
                .on().termination((f, c) -> cancelled.complete(null))
                .transform().bySkippingItemsWhile(i -> {
                    throw new QuietRuntimeException("failed");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void dropWhileStageShouldPropagateUpstreamErrorsWhileDropping() {
        await(Multi.createFrom().<Integer> failure(new QuietRuntimeException("failed"))
                .transform().bySkippingItemsWhile(i -> i < 3)
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void dropWhileStageShouldPropagateUpstreamErrorsAfterFinishedDropping() {
        await(infiniteStream()
                .onItem().invoke(i -> {
                    if (i == 4) {
                        throw new QuietRuntimeException("failed");
                    }
                })
                .transform().bySkippingItemsWhile(i -> i < 3)
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test
    public void dropWhileStageShouldNotRunPredicateOnceItsFinishedDropping() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3, 4)
                .transform().bySkippingItemsWhile(i -> {
                    if (i < 3) {
                        return true;
                    } else if (i == 4) {
                        throw new RuntimeException("4 was passed");
                    } else {
                        return false;
                    }
                })
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList(3, 4));
    }

    @Test
    public void dropWhileStageShouldAllowCompletionWhileDropping() {
        assertEquals(await(Multi.createFrom().items(1, 1, 1, 1)
                .transform().bySkippingItemsWhile(i -> i < 3)
                .collectItems().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void dropWhileStageShouldPropagateCancel() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .on().termination((f, c) -> cancelled.complete(null))
                .transform().bySkippingItemsWhile(i -> i < 3)
                .subscribe().with(new MultiAssertSubscriber<>(10, true));
        await(cancelled);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().items(LongStream.rangeClosed(1, elements).boxed())
                .transform().bySkippingItemsWhile(i -> false);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .transform().bySkippingItemsWhile(i -> false);
    }
}
