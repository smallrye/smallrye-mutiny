package io.smallrye.mutiny.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiScanTest {

    @Test
    public void testThatSupplierMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty().onItem().scan(null, (a, b) -> a));
    }

    @Test
    public void testThatScannerMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty().onItem().scan(() -> 1, null));
    }

    @Test
    public void testThatScannerMustNotBeNullWithoutSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty().onItem().scan(null));
    }

    @Test
    public void testWithSimplerScanner() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> b)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testWithSimplerScannerWithSupplier() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 2, (a, b) -> b)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(2, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testWithRequests() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

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
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(2);

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
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> null)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWithAScannerThrowingExceptionWithSupplier() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(2);

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
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 1, (a, b) -> null)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(NullPointerException.class, "");
    }
}
