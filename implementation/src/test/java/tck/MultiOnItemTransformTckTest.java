package tck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tck.Await.await;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiOnItemTransformTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void mapStageShouldMapElements() {
        assertEquals(await(Multi.createFrom().items(1, 2, 3)
                .map(Object::toString)
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList("1", "2", "3"));
    }

    @Test
    public void mapStageShouldHandleExceptions() {
        assertThrows(QuietRuntimeException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .map(foo -> {
                        throw new QuietRuntimeException("failed");
                    })
                    .collect().asList()
                    .subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Test
    public void mapStageShouldPropagateUpstreamExceptions() {
        assertThrows(QuietRuntimeException.class,
                () -> await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                        .map(Function.identity())
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void mapStageShouldFailIfNullReturned() {
        assertThrows(NullPointerException.class, () -> {
            CompletableFuture<Void> cancelled = new CompletableFuture<>();
            CompletionStage<List<Object>> result = infiniteStream()
                    .onTermination().invoke(() -> cancelled.complete(null))
                    .map(t -> null)
                    .collect().asList().subscribeAsCompletionStage();
            await(cancelled);
            await(result);
        });
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .map(Function.identity());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .map(Function.identity());
    }

}
