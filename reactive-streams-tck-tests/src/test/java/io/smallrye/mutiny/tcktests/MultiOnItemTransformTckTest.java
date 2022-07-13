package io.smallrye.mutiny.tcktests;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiOnItemTransformTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void mapStageShouldMapElements() {
        Assert.assertEquals(Await.await(Multi.createFrom().items(1, 2, 3)
                .map(Object::toString)
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList("1", "2", "3"));
    }

    @Test
    public void mapStageShouldHandleExceptions() {
        Assert.assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .map(foo -> {
                        throw new QuietRuntimeException("failed");
                    })
                    .collect().asList()
                    .subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }

    @Test
    public void mapStageShouldPropagateUpstreamExceptions() {
        Assert.assertThrows(QuietRuntimeException.class,
                () -> Await.await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                        .map(Function.identity())
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void mapStageShouldFailIfNullReturned() {
        Assert.assertThrows(NullPointerException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .map(t -> null)
                    .collect().asList().subscribeAsCompletionStage();
            Await.await(cancelled);
            Await.await(result);
        });
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .map(Function.identity());
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .map(Function.identity());
    }

}
