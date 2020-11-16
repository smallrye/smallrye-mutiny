package io.smallrye.mutiny.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiCreateFromDeferredSupplierTest {

    @Test
    public void testThatTheSupplierCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().deferred(null));
    }

    @Test
    public void testWhenTheSupplierProduceNull() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> deferred(() -> null).subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWhenTheSupplierThrowsAnException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> deferred(() -> {
            throw new IllegalStateException("boom");
        }).subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithASupplierProducingOne() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().deferred(() -> Multi.createFrom().item(1)).subscribe(subscriber);

        subscriber.assertCompleted()
                .assertItems(1);
    }

    @Test
    public void testThatEachSubscriberHasItsOwn() {
        AtomicInteger count = new AtomicInteger();

        Multi<Integer> multi = Multi.createFrom().deferred(() -> Multi.createFrom().item(count.incrementAndGet()));

        AssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<Integer> s3 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));

        s1.assertItems(1).assertCompleted();
        s2.assertItems(2).assertCompleted();
        s3.assertItems(3).assertCompleted();
    }
}
