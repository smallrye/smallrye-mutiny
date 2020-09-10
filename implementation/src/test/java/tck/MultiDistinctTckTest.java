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
import io.smallrye.mutiny.helpers.Subscriptions;

public class MultiDistinctTckTest extends AbstractPublisherTck<Long> {
    @Test
    public void distinctStageShouldReturnDistinctElements() {
        assertEquals(await(
                Multi.createFrom().items(1, 2, 2, 3, 2, 1, 3)
                        .transform().byDroppingDuplicates()
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void distinctStageShouldReturnAnEmptyStreamWhenCalledOnEmptyStreams() {
        assertEquals(await(Multi.createFrom().empty()
                .transform().byDroppingDuplicates()
                .collectItems().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void distinctStageShouldPropagateUpstreamExceptions() {
        assertThrows(QuietRuntimeException.class,
                () -> await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                        .transform().byDroppingDuplicates()
                        .collectItems().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void distinctStageShouldPropagateExceptionsThrownByEquals() {
        assertThrows(QuietRuntimeException.class, () -> {
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
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .transform().byDroppingDuplicates().collectItems().asList().subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Test
    public void distinctStageShouldPropagateCancel() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .transform().byDroppingDuplicates().subscribe()
                .withSubscriber(new Subscriptions.CancelledSubscriber<>());
        await(cancelled);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .transform().byDroppingDuplicates();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .transform().byDroppingDuplicates();
    }

}
