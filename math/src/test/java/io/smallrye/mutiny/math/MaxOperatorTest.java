package io.smallrye.mutiny.math;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MaxOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().<Long> empty()
                .plug(Math.max())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitCompletion()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().<Long> nothing()
                .plug(Math.max())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithItems() {
        AssertSubscriber<String> subscriber = Multi.createFrom().items("e", "b", "c", "f", "g", "e")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.max())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems("e", "f", "g")
                .request(10)
                .awaitCompletion()
                .assertItems("e", "f", "g");
    }

    @Test
    public void testLatest() {
        String max = Multi.createFrom().items("e", "b", "c", "f", "g", "e")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.max())
                .collect().last()
                .await().indefinitely();

        Assertions.assertEquals("g", max);
    }

    @RepeatedTest(1000)
    public void testWithItemsAndFailure() {
        AssertSubscriber<String> subscriber = Multi.createFrom().items("a", "b", "c", "c", "a", "c", "e", "a", "q", "x")
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .onCompletion().failWith(new Exception("boom"))
                .plug(Math.max())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .assertItems("a", "b", "c")
                .request(10)
                .awaitFailure();

        // We can't be completely deterministic as the failure is sent immediately (not following the request and emission)
        assertThat(subscriber.getItems()).startsWith("a", "b", "c").hasSizeGreaterThanOrEqualTo(5);
    }
}
