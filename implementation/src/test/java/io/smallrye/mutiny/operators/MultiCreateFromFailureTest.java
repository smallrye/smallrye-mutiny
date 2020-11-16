package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiCreateFromFailureTest {

    @Test
    public void testThatFailureCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().failure((Throwable) null));
    }

    @Test
    public void testThatFailureSupplierCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().failure((Supplier<Throwable>) null));
    }

    @Test
    public void testWithException() {
        AssertSubscriber<String> subscriber = Multi.createFrom().<String> failure(new IOException("boom"))
                .subscribe()
                .withSubscriber(AssertSubscriber.create());
        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<String> failure = Multi.createFrom()
                .failure(() -> new IOException("boom-" + count.incrementAndGet()));
        AssertSubscriber<String> subscriber1 = failure.subscribe().withSubscriber(AssertSubscriber.create());
        AssertSubscriber<String> subscriber2 = failure.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertFailedWith(IOException.class, "boom-1");
        subscriber2.assertFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().failure(() -> {
            throw new IllegalStateException("boom");
        });
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertTerminated().assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().failure(() -> null);
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.getFailure()).isNotNull().isInstanceOf(NullPointerException.class);
    }
}
