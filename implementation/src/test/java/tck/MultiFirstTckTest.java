package tck;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static tck.Await.await;

import java.util.concurrent.CompletableFuture;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiFirstTckTest extends AbstractTck {
    @Test
    public void findFirstStageShouldFindTheFirstElement() {
        int res = await(
                Multi.createFrom().items(1, 2, 3)
                        .collectItems().first()
                        .subscribeAsCompletionStage());
        assertEquals(res, 1);
    }

    @Test
    public void findFirstStageShouldFindTheFirstElementInSingleElementStream() {
        int result = await(Multi.createFrom().item(1)
                .collectItems().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
    }

    @Test
    public void findFirstStageShouldReturnEmptyForEmptyStream() {
        assertNull(await(Multi.createFrom().items()
                .collectItems().first().subscribeAsCompletionStage()), null);
    }

    @Test
    public void findFirstStageShouldCancelUpstream() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        int result = await(infiniteStream()
                .on().termination(() -> cancelled.complete(null))
                .collectItems().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
        await(cancelled);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void findFirstStageShouldPropagateErrors() {
        await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                .collectItems().first().subscribeAsCompletionStage());
    }

    @Test
    public void findFirstStageShouldBeReusable() {
        int result = await(Multi.createFrom().items(1, 2, 3)
                .collectItems().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
    }
}
