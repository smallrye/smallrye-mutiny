package tck;

import static org.testng.Assert.assertEquals;
import static tck.Await.await;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiMapTckTest extends AbstractPublisherTck<Integer> {

    @Test
    public void mapStageShouldMapElements() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3)
                .map(Object::toString)
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList("1", "2", "3"));
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void mapStageShouldHandleExceptions() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<List<Object>> result = infiniteStream()
                .on().termination((fail, can) -> cancelled.complete(null))
                .map(foo -> {
                    throw new QuietRuntimeException("failed");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void mapStageShouldPropagateUpstreamExceptions() {
        await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                .map(Function.identity())
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void mapStageShouldFailIfNullReturned() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<List<Object>> result = infiniteStream()
                .on().termination((fail, can) -> cancelled.complete(null))
                .map(t -> null)
                .collectItems().asList().subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return Multi.createFrom().items(IntStream.rangeClosed(1, (int) elements).boxed())
                .map(Function.identity());
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return Multi.createFrom().<Integer> failure(new RuntimeException("failed"))
                .map(Function.identity());
    }

}
