package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import org.junit.Test;

public class MultiScanTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatSupplierMustNotBeNull() {
        Multi.createFrom().empty().onItem().scan(null, (a, b) -> a);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatScannerMustNotBeNull() {
        Multi.createFrom().empty().onItem().scan(() -> 1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatScannerMustNotBeNullWithoutSupplier() {
        Multi.createFrom().empty().onItem().scan(null);
    }

    @Test
    public void testWithSimplerScanner() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> b)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testWithSimplerScannerWithSupplier() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 2, (a, b) -> b)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(2, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testWithRequests() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> b)
                .subscribe().withSubscriber(subscriber);

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
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWithAScannerReturningNull() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> null)
                .subscribe().withSubscriber(subscriber);

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
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWithAScannerReturningNullWithSupplier() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 1, (a, b) -> null)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(NullPointerException.class, "");
    }
}
