package io.smallrye.mutiny.operators;

import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiCreateFromDeferredSupplierTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheSupplierCannotBeNull() {
        Multi.createFrom().deferred(null);
    }

    @Test
    public void testWhenTheSupplierProduceNull() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> deferred(() -> null).subscribe(subscriber);

        subscriber
                .assertHasNotCompleted()
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWhenTheSupplierThrowsAnException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> deferred(() -> {
            throw new IllegalStateException("boom");
        }).subscribe(subscriber);

        subscriber
                .assertHasNotCompleted()
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithASupplierProducingOne() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().deferred(() -> Multi.createFrom().item(1)).subscribe(subscriber);

        subscriber.assertCompletedSuccessfully()
                .assertReceived(1)
                .assertHasNotFailed();
    }

    @Test
    public void testThatEachSubscriberHasItsOwn() {
        AtomicInteger count = new AtomicInteger();

        Multi<Integer> multi = Multi.createFrom().deferred(() -> Multi.createFrom().item(count.incrementAndGet()));

        AssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<Integer> s3 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));

        s1.assertReceived(1).assertCompletedSuccessfully();
        s2.assertReceived(2).assertCompletedSuccessfully();
        s3.assertReceived(3).assertCompletedSuccessfully();
    }
}
