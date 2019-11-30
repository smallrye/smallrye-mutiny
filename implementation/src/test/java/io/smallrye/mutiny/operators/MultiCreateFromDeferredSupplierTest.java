package io.smallrye.mutiny.operators;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromDeferredSupplierTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatTheSupplierCannotBeNull() {
        Multi.createFrom().deferred(null);
    }

    @Test
    public void testWhenTheSupplierProduceNull() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        Multi.createFrom().<Integer> deferred(() -> null).subscribe(ts);

        ts
                .assertHasNotCompleted()
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWhenTheSupplierThrowsAnException() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        Multi.createFrom().<Integer> deferred(() -> {
            throw new IllegalStateException("boom");
        }).subscribe(ts);

        ts
                .assertHasNotCompleted()
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithASupplierProducingOne() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(1);

        Multi.createFrom().deferred(() -> Multi.createFrom().item(1)).subscribe(ts);

        ts.assertCompletedSuccessfully()
                .assertReceived(1)
                .assertHasNotFailed();
    }

    @Test
    public void testThatEachSubscriberHasItsOwn() {
        AtomicInteger count = new AtomicInteger();

        Multi<Integer> multi = Multi.createFrom().deferred(() -> Multi.createFrom().item(count.incrementAndGet()));

        MultiAssertSubscriber<Integer> s1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<Integer> s2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<Integer> s3 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));

        s1.assertReceived(1).assertCompletedSuccessfully();
        s2.assertReceived(2).assertCompletedSuccessfully();
        s3.assertReceived(3).assertCompletedSuccessfully();
    }
}
