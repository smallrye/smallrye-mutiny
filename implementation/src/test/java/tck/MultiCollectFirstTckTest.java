package tck;

import static org.junit.jupiter.api.Assertions.*;
import static tck.Await.await;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;

public class MultiCollectFirstTckTest extends AbstractTck {
    @Test
    public void findFirstStageShouldFindTheFirstElement() {
        int res = await(
                Multi.createFrom().items(1, 2, 3)
                        .collect().first()
                        .subscribeAsCompletionStage());
        assertEquals(res, 1);
    }

    @Test
    public void findFirstStageShouldFindTheFirstElementInSingleElementStream() {
        int result = await(Multi.createFrom().item(1)
                .collect().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
    }

    @Test
    public void findFirstStageShouldReturnEmptyForEmptyStream() {
        assertNull(await(Multi.createFrom().items()
                .collect().first().subscribeAsCompletionStage()));
    }

    @Test
    public void findFirstStageShouldCancelUpstream() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        int result = await(infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .collect().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
        await(cancelled);
    }

    @Test
    public void findFirstStageShouldPropagateErrors() {
        assertThrows(QuietRuntimeException.class,
                () -> await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                        .collect().first().subscribeAsCompletionStage()));
    }

    @Test
    public void findFirstStageShouldBeReusable() {
        int result = await(Multi.createFrom().items(1, 2, 3)
                .collect().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
    }
}
