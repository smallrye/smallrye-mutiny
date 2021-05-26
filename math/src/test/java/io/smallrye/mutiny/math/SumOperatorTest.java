package io.smallrye.mutiny.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class SumOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().<Integer> empty()
                .plug(Math.sum())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.awaitItems(1)
                .assertItems(0.0d)
                .awaitCompletion();
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().<Long> nothing()
                .plug(Math.sum())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithIntegerItems() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.sum())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(1.0, 3.0, 6.0)
                .request(10)
                .awaitCompletion()
                .assertItems(1.0, 3.0, 6.0, 10.0, 15.0, 21.0);
    }

    @Test
    public void testLatest() {
        double sum = Multi.createFrom().items(1, 2, 3, 4, 5, 6)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.sum())
                .collect().last()
                .await().indefinitely();

        Assertions.assertEquals(21.0, sum);
    }

    @Test
    public void testWithLongItems() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().items(1L, 2L, 3L, 4L, 5L, 6L)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.sum())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(1.0, 3.0, 6.0)
                .request(10)
                .awaitCompletion()
                .assertItems(1.0, 3.0, 6.0, 10.0, 15.0, 21.0);
    }

    @Test
    public void testWithDoubleItems() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().items(1.1, 2.0, 3.5, 4.0, 5.0, 6.0)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.sum())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(1.1, 3.1, 6.6)
                .request(10)
                .awaitCompletion()
                .assertItems(1.1, 3.1, 6.6, 10.6, 15.6, 21.6);
    }

    @RepeatedTest(1000)
    public void testWithItemsAndFailure() {
        AssertSubscriber<Double> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().items(1, 2, 3, 4, 5, 6),
                Multi.createFrom().failure(new Exception("boom")))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.sum())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(1.0, 3.0, 6.0)
                .request(10)
                .awaitFailure()
                .assertItems(1.0, 3.0, 6.0, 10.0, 15.0, 21.0);
    }

}
