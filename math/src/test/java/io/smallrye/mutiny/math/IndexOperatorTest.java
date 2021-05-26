package io.smallrye.mutiny.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;

public class IndexOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<Tuple2<Long, String>> subscriber = Multi.createFrom().<String> empty()
                .plug(Math.index())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitCompletion()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Tuple2<Long, String>> subscriber = Multi.createFrom().<String> nothing()
                .plug(Math.index())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithItems() {
        AssertSubscriber<Tuple2<Long, String>> subscriber = Multi.createFrom().items("a", "b", "c", "d", "e")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.index())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(Tuple2.of(0L, "a"), Tuple2.of(1L, "b"), Tuple2.of(2L, "c"))
                .request(10)
                .awaitItems(5)
                .assertItems(Tuple2.of(0L, "a"), Tuple2.of(1L, "b"), Tuple2.of(2L, "c"),
                        Tuple2.of(3L, "d"), Tuple2.of(4L, "e"))
                .awaitCompletion();
    }

    @Test
    public void testLatest() {
        Tuple2<Long, String> index = Multi.createFrom().items("a", "b", "c", "d", "e")
                .plug(Math.index())
                .collect().last()
                .await().indefinitely();

        Assertions.assertEquals(Tuple2.of(4L, "e"), index);
    }

    @RepeatedTest(1000)
    public void testWithItemsAndFailure() {
        AssertSubscriber<Tuple2<Long, String>> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().items("a", "b", "c", "d", "e"),
                Multi.createFrom().failure(new Exception("boom")))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.index())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(Tuple2.of(0L, "a"), Tuple2.of(1L, "b"), Tuple2.of(2L, "c"))
                .request(10)
                .awaitFailure()
                .assertItems(Tuple2.of(0L, "a"), Tuple2.of(1L, "b"), Tuple2.of(2L, "c"),
                        Tuple2.of(3L, "d"), Tuple2.of(4L, "e"));
    }
}
