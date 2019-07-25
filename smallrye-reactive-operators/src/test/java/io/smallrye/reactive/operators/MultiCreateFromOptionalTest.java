package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import org.assertj.core.data.Index;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiCreateFromOptionalTest {

    @SuppressWarnings("OptionalAssignedToNull")
    @Test(expected = IllegalArgumentException.class)
    public void testThatTheOptionalCannotBeNull() {
        Multi.createFrom().optional((Optional<String>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatOptionalSupplierCannotBeNull() {
        Multi.createFrom().optional((Supplier<Optional<String>>) null);
    }

    @Test
    public void testWithAValue() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().optional(Optional.of("hello")).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithEmpty() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().<String>optional(Optional.empty()).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertHasNoResults();
    }

    @Test
    public void testWithAValueProducedInSupplier() {
        AtomicInteger count = new AtomicInteger();

        Multi<String> multi  = Multi.createFrom()
                .optional(() -> Optional.of("hello-" + count.incrementAndGet()));
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertReceived("hello-1");
        subscriber2.assertHasNoResults().assertNotTerminated().request(20)
                .assertCompletedSuccessfully().assertReceived("hello-2");
    }

    @Test
    public void testWithEmptyProducedInSupplier() {
        Multi<String> multi  = Multi.createFrom().optional(Optional::empty);
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertHasNoResults();
        subscriber2.assertHasNoResults().assertCompletedSuccessfully();
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi  = Multi.createFrom().optional(() -> {
            throw new IllegalStateException("boom");
        });
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @SuppressWarnings("OptionalAssignedToNull")
    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi  = Multi.createFrom().optional(() -> null);
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1).allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }
}
