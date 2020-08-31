package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiCreateFromOptionalTest {

    @SuppressWarnings("OptionalAssignedToNull")
    @Test
    public void testThatTheOptionalCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().optional((Optional<String>) null));
    }

    @Test
    public void testThatOptionalSupplierCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().optional((Supplier<Optional<String>>) null));
    }

    @Test
    public void testWithAValue() {
        AssertSubscriber<String> subscriber = Multi.createFrom().optional(Optional.of("hello")).subscribe()
                .withSubscriber(AssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithEmpty() {
        AssertSubscriber<String> subscriber = Multi.createFrom().<String> optional(Optional.empty()).subscribe()
                .withSubscriber(AssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithAValueProducedInSupplier() {
        AtomicInteger count = new AtomicInteger();

        Multi<String> multi = Multi.createFrom()
                .optional(() -> Optional.of("hello-" + count.incrementAndGet()));
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(AssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertReceived("hello-1");
        subscriber2.assertHasNotReceivedAnyItem().assertNotTerminated().request(20)
                .assertCompletedSuccessfully().assertReceived("hello-2");
    }

    @Test
    public void testWithEmptyProducedInSupplier() {
        Multi<String> multi = Multi.createFrom().optional(Optional::empty);
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(AssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        subscriber2.assertHasNotReceivedAnyItem().assertCompletedSuccessfully();
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().optional(() -> {
            throw new IllegalStateException("boom");
        });
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @SuppressWarnings("OptionalAssignedToNull")
    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().optional(() -> null);
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }
}
