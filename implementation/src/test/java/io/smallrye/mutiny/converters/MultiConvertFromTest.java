package io.smallrye.mutiny.converters;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.BuiltinConverters;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromCompletionStageWithValue() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);

        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .completionStage(valued)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompleted().assertItems(1);
    }

    @Test
    public void testCreatingFromCompletionStageWithEmpty() {
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);

        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(BuiltinConverters.fromCompletionStage(), empty)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        Multi.createFrom().completionStage(empty);

        subscriber.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromCompletionStageWithException() {
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(BuiltinConverters.fromCompletionStage(), boom)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(Exception.class, "boom");
    }
}
