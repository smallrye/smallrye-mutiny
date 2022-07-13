package io.smallrye.mutiny.tcktests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;

public class MultiSelectDistinctTckTest extends AbstractPublisherTck<Long> {
    @Test
    public void distinctStageShouldReturnDistinctElements() {
        Assert.assertEquals(Await.await(
                Multi.createFrom().items(1, 2, 2, 3, 2, 1, 3)
                        .select().distinct()
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void distinctStageShouldReturnAnEmptyStreamWhenCalledOnEmptyStreams() {
        Assert.assertEquals(Await.await(Multi.createFrom().empty()
                .select().distinct()
                .collect().asList()
                .subscribeAsCompletionStage()), Collections.emptyList());
    }

    @Test
    public void distinctStageShouldPropagateUpstreamExceptions() {
        Assert.assertThrows(QuietRuntimeException.class,
                () -> Await.await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                        .select().distinct()
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void distinctStageShouldPropagateExceptionsThrownByEquals() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
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
                    .select().distinct()
                    .collect().asList().subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }

    @Test
    public void distinctStageShouldPropagateCancel() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .select().distinct().subscribe()
                .withSubscriber(new Subscriptions.CancelledSubscriber<>());
        Await.await(cancelled);
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .select().distinct();
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .select().distinct();
    }

}
