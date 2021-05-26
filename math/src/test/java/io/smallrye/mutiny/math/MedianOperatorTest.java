package io.smallrye.mutiny.math;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MedianOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().<Integer> empty()
                .plug(Math.median())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitCompletion()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().<Long> nothing()
                .plug(Math.median())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithIntegerItems() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.median())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(1.0, 1.5, 2.0)
                .request(10)
                .awaitCompletion()
                .assertItems(1.0, 1.5, 2.0, 2.5, 3.0, 3.5);
    }

    @Test
    public void testWithIntegerItemsShuffled() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().items(6, 1, 2, 5, 3, 1)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.median())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(6.0, 3.5, 2.0)
                .request(10)
                .awaitCompletion()
                .assertItems(6.0, 3.5, 2.0, 3.5, 3.0, 2.5);
    }

    @Test
    public void testLatest() {
        List<Integer> list = Arrays.asList(3, 4, 1, 2, 6, 7, 9, 2, 4, 5, 2, 7, 9, 10, 2, 5, 3, 4, 3, 8, 4, 7, 9);
        Collections.shuffle(list);
        double median = Multi.createFrom().iterable(list)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.median())
                .collect().last()
                .await().indefinitely();

        Assertions.assertEquals(4.0, median);
    }

    @Test
    public void testWithLongItems() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().items(6L, 1L, 2L, 5L, 3L, 1L)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.median())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(6.0, 3.5, 2.0)
                .request(10)
                .awaitCompletion()
                .assertItems(6.0, 3.5, 2.0, 3.5, 3.0, 2.5);
    }

    @Test
    public void testWithDoubleItems() {
        AssertSubscriber<Double> subscriber = Multi.createFrom().items(6.0, 1.0, 2.0, 5.0, 3.0, 1.0)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.median())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(6.0, 3.5, 2.0)
                .request(10)
                .awaitCompletion()
                .assertItems(6.0, 3.5, 2.0, 3.5, 3.0, 2.5);
    }

    @RepeatedTest(1000)
    public void testWithItemsAndFailure() {
        AssertSubscriber<Double> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().items(6, 1, 2, 5, 3, 1),
                Multi.createFrom().failure(new Exception("boom")))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.median())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(6.0, 3.5, 2.0)
                .request(10)
                .awaitFailure()
                .assertItems(6.0, 3.5, 2.0, 3.5, 3.0, 2.5);
    }

}
