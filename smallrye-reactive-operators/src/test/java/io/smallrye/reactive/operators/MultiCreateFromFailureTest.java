package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class MultiCreateFromFailureTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatFailureCannotBeNull() {
        Multi.createFrom().failure((Exception) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatFailureSupplierCannotBeNull() {
        Multi.createFrom().failure((Supplier<Throwable>) null);
    }

    @Test
    public void testWithException() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().<String>failure(new IOException("boom")).subscribe()
                .withSubscriber(MultiAssertSubscriber.create());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<String> failure = Multi.createFrom()
                .failure(() -> new IOException("boom-" + count.incrementAndGet()));
        MultiAssertSubscriber<String> subscriber1 = failure.subscribe().withSubscriber(MultiAssertSubscriber.create());
        MultiAssertSubscriber<String> subscriber2 = failure.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertHasFailedWith(IOException.class, "boom-1");
        subscriber2.assertHasFailedWith(IOException.class, "boom-2");
    }
}
