package tck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiSkipItemsWhileTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void dropWhileStageShouldSupportDroppingElements() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3, 4, 0)
                .transform().bySkippingItemsWhile(i -> i < 3)
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList(3, 4, 0));
    }

    @Test
    public void dropWhileStageShouldHandleErrors() {
        assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Integer>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .transform().bySkippingItemsWhile(i -> {
                        throw new QuietRuntimeException("failed");
                    })
                    .collectItems().asList()
                    .subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Test
    public void dropWhileStageShouldPropagateUpstreamErrorsWhileDropping() {
        assertThrows(QuietRuntimeException.class,
                () -> await(Multi.createFrom().<Integer> failure(new QuietRuntimeException("failed"))
                        .transform().bySkippingItemsWhile(i -> i < 3)
                        .collectItems().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void dropWhileStageShouldPropagateUpstreamErrorsAfterFinishedDropping() {
        assertThrows(QuietRuntimeException.class, () -> await(infiniteStream()
                .onItem().invoke(i -> {
                    if (i == 4) {
                        throw new QuietRuntimeException("failed");
                    }
                })
                .transform().bySkippingItemsWhile(i -> i < 3)
                .collectItems().asList()
                .subscribeAsCompletionStage()));
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
                .onTermination().invoke(() -> cancelled.complete(null))
                .transform().bySkippingItemsWhile(i -> i < 3)
                .subscribe().withSubscriber(new AssertSubscriber<>(10, true));
        await(cancelled);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .transform().bySkippingItemsWhile(i -> false);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .transform().bySkippingItemsWhile(i -> false);
    }
}
