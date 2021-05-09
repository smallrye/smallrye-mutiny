package io.smallrye.mutiny.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MinOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().<Long> empty()
                .plug(Math.min())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitCompletion()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().<Long> nothing()
                .plug(Math.min())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithItems() {
        AssertSubscriber<String> subscriber = Multi.createFrom().items("e", "b", "c", "c", "a", "e")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.min())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems("e", "b", "a")
                .request(10)
                .awaitCompletion()
                .assertItems("e", "b", "a");
    }

    @Test
    public void testLatest() {
        String min = Multi.createFrom().items("e", "b", "c", "c", "a", "e")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.min())
                .collect().last()
                .await().indefinitely();

        Assertions.assertEquals("a", min);
    }

    @Test
    public void testWithItemsAndFailure() {
        AssertSubscriber<String> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().items("e", "b", "c", "c", "a", "e", "a", "v", "x"),
                Multi.createFrom().failure(new Exception("boom")))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.min())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems("e", "b", "a")
                .request(10)
                .awaitFailure()
                .assertItems("e", "b", "a");
    }
}
