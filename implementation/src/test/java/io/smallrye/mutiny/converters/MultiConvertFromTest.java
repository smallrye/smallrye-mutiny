package io.smallrye.mutiny.converters;

import java.util.concurrent.CompletableFuture;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.BuiltinConverters;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromCompletionStageWithValue() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);

        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .completionStage(valued)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromCompletionStageWithEmpty() {
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);

        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(BuiltinConverters.fromCompletionStage(), empty)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        Multi.createFrom().completionStage(empty);

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromCompletionStageWithException() {
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(BuiltinConverters.fromCompletionStage(), boom)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertHasFailedWith(Exception.class, "boom");
    }
}
