package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromFailureTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFailureCannotBeNull() {
        Multi.createFrom().failure(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFailureSupplierCannotBeNull() {
        Multi.createFrom().deferredFailure(null);
    }

    @Test
    public void testWithException() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().<String> failure(new IOException("boom"))
                .subscribe()
                .with(MultiAssertSubscriber.create());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<String> failure = Multi.createFrom()
                .deferredFailure(() -> new IOException("boom-" + count.incrementAndGet()));
        MultiAssertSubscriber<String> subscriber1 = failure.subscribe().with(MultiAssertSubscriber.create());
        MultiAssertSubscriber<String> subscriber2 = failure.subscribe().with(MultiAssertSubscriber.create());
        subscriber1.assertHasFailedWith(IOException.class, "boom-1");
        subscriber2.assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().deferredFailure(() -> {
            throw new IllegalStateException("boom");
        });
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().with(MultiAssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().deferredFailure(() -> null);
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().with(MultiAssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }
}
