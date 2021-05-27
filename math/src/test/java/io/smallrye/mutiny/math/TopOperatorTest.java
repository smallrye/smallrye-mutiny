package io.smallrye.mutiny.math;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class TopOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<List<Long>> subscriber = Multi.createFrom().<Long> empty()
                .plug(Math.top(10))
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitCompletion()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<List<Long>> subscriber = Multi.createFrom().<Long> nothing()
                .plug(Math.top(3))
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithItems() {
        AssertSubscriber<List<String>> subscriber = Multi.createFrom()
                .items("a", "b", "a", "e", "f", "b", "a", "g", "g", "e", "z")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.top(3))
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(list("a"), list("b", "a"), list("e", "b", "a"))
                .request(10)
                .awaitItems(6)
                .assertItems(list("a"), list("b", "a"), list("e", "b", "a"), list("f", "e", "b"),
                        list("g", "f", "e"), list("z", "g", "f"))
                .awaitCompletion();
    }

    @Test
    public void testLast() {
        List<String> last = Multi.createFrom().items("a", "b", "a", "e", "f", "b", "a", "g", "g", "e", "z")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.top(3))
                .collect().last()
                .await().indefinitely();

        Assertions.assertEquals(list("z", "g", "f"), last);
    }

    @Test
    public void testEquality() {
        AssertSubscriber<List<String>> subscriber = Multi.createFrom()
                .items("a", "b", "c", "a", "b", "c", "c", "b", "a", "a", "c")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.top(3))
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(list("a"), list("b", "a"), list("c", "b", "a"))
                .request(10)
                .awaitCompletion()
                .assertItems(list("a"), list("b", "a"), list("c", "b", "a"));
    }

    @RepeatedTest(1000)
    public void testWithItemsAndFailure() {
        AssertSubscriber<List<String>> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().items("a", "b", "a", "e", "f", "b", "g", "g", "e", "z"),
                Multi.createFrom().failure(new Exception("boom")))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.top(3))
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems(list("a"), list("b", "a"), list("e", "b", "a"))
                .request(10)
                .awaitFailure();
    }

    private static List<String> list(String... items) {
        return Arrays.asList(items);
    }

}
