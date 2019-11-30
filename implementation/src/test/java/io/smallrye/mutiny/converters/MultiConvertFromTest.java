package io.smallrye.mutiny.converters;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.BuiltinConverters;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromCompletionStageWithValue() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);

        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .completionStage(valued)
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromCompletionStageWithEmpty() {
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);

        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(BuiltinConverters.fromCompletionStage(), empty)
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        Multi<Void> m2 = Multi.createFrom().completionStage(empty);

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromCompletionStageWithException() {
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(BuiltinConverters.fromCompletionStage(), boom)
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(Exception.class, "boom");
    }
}
