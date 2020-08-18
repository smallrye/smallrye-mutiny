package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiCreateFromFailureTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFailureCannotBeNull() {
        Multi.createFrom().failure((Throwable) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFailureSupplierCannotBeNull() {
        Multi.createFrom().failure((Supplier<Throwable>) null);
    }

    @Test
    public void testWithException() {
        AssertSubscriber<String> subscriber = Multi.createFrom().<String> failure(new IOException("boom"))
                .subscribe()
                .withSubscriber(AssertSubscriber.create());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<String> failure = Multi.createFrom()
                .failure(() -> new IOException("boom-" + count.incrementAndGet()));
        AssertSubscriber<String> subscriber1 = failure.subscribe().withSubscriber(AssertSubscriber.create());
        AssertSubscriber<String> subscriber2 = failure.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertHasFailedWith(IOException.class, "boom-1");
        subscriber2.assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().failure(() -> {
            throw new IllegalStateException("boom");
        });
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().failure(() -> null);
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }
}
