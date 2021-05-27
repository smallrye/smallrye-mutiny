package io.smallrye.mutiny.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class CountOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().empty()
                .plug(Math.count())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.awaitItems(1)
                .assertItems(0L)
                .awaitCompletion();
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().nothing()
                .plug(Math.count())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithItems() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().items("a", "b", "c", "d", "e")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.count())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(1L, 2L, 3L)
                .request(10)
                .awaitItems(5)
                .assertItems(1L, 2L, 3L, 4L, 5L)
                .awaitCompletion();
    }

    @Test
    public void testLatest() {
        double count = Multi.createFrom().items("a", "b", "c", "d", "e")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.count())
                .collect().last()
                .await().indefinitely();

        Assertions.assertEquals(5, count);
    }

    @RepeatedTest(1000)
    public void testWithItemsAndFailure() {
        AssertSubscriber<Long> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().items("a", "b", "c", "d", "e"),
                Multi.createFrom().failure(new Exception("boom")))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.count())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(1L, 2L, 3L)
                .request(10)
                .awaitFailure()
                .assertItems(1L, 2L, 3L, 4L, 5L);
    }

}
