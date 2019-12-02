package tck;

import static org.testng.Assert.assertEquals;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;

public class MultiDistinctTckTest extends AbstractPublisherTck<Integer> {
    @Test
    public void distinctStageShouldReturnDistinctElements() {
        assertEquals(await(
                Multi.createFrom().items(1, 2, 2, 3, 2, 1, 3)
                        .transform().byKeepingDistinctItems()
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void distinctStageShouldReturnAnEmptyStreamWhenCalledOnEmptyStreams() {
        assertEquals(await(Multi.createFrom().empty()
                .transform().byKeepingDistinctItems()
                .collectItems().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void distinctStageShouldPropagateUpstreamExceptions() {
        await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                .transform().byKeepingDistinctItems()
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void distinctStageShouldPropagateExceptionsThrownByEquals() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        class ObjectThatThrowsFromEquals {
            @Override
            public int hashCode() {
                return 1;
            }

            @Override
            public boolean equals(Object obj) {
                throw new QuietRuntimeException("failed");
            }
        }
        CompletionStage<List<ObjectThatThrowsFromEquals>> result = Multi.createFrom().items(
                new ObjectThatThrowsFromEquals(), new ObjectThatThrowsFromEquals())
                .on().termination((e, c) -> cancelled.complete(null))
                .transform().byKeepingDistinctItems().collectItems().asList().subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Test
    public void distinctStageShouldPropagateCancel() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .on().termination((e, c) -> cancelled.complete(null))
                .transform().byKeepingDistinctItems().subscribe().with(new Subscriptions.CancelledSubscriber<>());
        await(cancelled);
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return Multi.createFrom().items(IntStream.rangeClosed(1, (int) elements).boxed())
                .transform().byKeepingDistinctItems();
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return Multi.createFrom().<Integer> failure(new RuntimeException("failed"))
                .transform().byKeepingDistinctItems();
    }
}
