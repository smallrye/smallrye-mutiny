package io.smallrye.mutiny.operators;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiScanTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatSupplierMustNotBeNull() {
        Multi.createFrom().empty().onItem().scan(null, (a, b) -> a);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatScannerMustNotBeNull() {
        Multi.createFrom().empty().onItem().scan(() -> 1, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatScannerMustNotBeNullWithoutSupplier() {
        Multi.createFrom().empty().onItem().scan(null);
    }

    @Test
    public void testWithSimplerScanner() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> b)
                .subscribe().with(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testWithSimplerScannerWithSupplier() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 2, (a, b) -> b)
                .subscribe().with(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(2, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testWithRequests() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> b)
                .subscribe().with(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated();

        subscriber.request(5)
                .assertReceived(1, 2, 3, 4, 5)
                .assertNotTerminated();

        subscriber.request(5)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithAScannerThrowingException() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().with(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWithAScannerReturningNull() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> null)
                .subscribe().with(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWithAScannerThrowingExceptionWithSupplier() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 1, (a, b) -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().with(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWithAScannerReturningNullWithSupplier() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 1, (a, b) -> null)
                .subscribe().with(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(NullPointerException.class, "");
    }
}
