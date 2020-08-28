package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class UniCreateFromDeferredSupplierTest {

    @Test
    public void testWithMultipleSubscriptions() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> s = Uni.createFrom().deferred(() -> Uni.createFrom().item(counter.incrementAndGet()));

        for (int i = 1; i < 100; i++) {
            assertThat(s.await().indefinitely()).isEqualTo(i);
        }
    }

    @Test
    public void testWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().deferred(null));
    }

    @Test
    public void testWithASupplierProducingNull() {
        Uni<Integer> s = Uni.createFrom().deferred(() -> null);
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        s.subscribe().withSubscriber(subscriber);
        subscriber.assertFailure(NullPointerException.class, "");
    }

    @Test
    public void testWithASupplierThrowingAnException() {
        Uni<Integer> s = Uni.createFrom().deferred(() -> {
            throw new IllegalStateException("boom");
        });
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        s.subscribe().withSubscriber(subscriber);
        subscriber.assertFailure(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithSharedState() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        AtomicInteger shared = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().deferred(() -> shared,
                state -> Uni.createFrom().item(state.incrementAndGet()));

        assertThat(shared).hasValue(0);
        uni.subscribe().withSubscriber(s1);
        assertThat(shared).hasValue(1);
        s1.assertCompletedSuccessfully().assertItem(1);
        uni.subscribe().withSubscriber(s2);
        assertThat(shared).hasValue(2);
        s2.assertCompletedSuccessfully().assertItem(2);
    }

    @Test
    public void testWithSharedStateProducingFailure() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> {
            throw new IllegalStateException("boom");
        };

        Uni<Integer> uni = Uni.createFrom().deferred(boom,
                page -> Uni.createFrom().item(page.getAndIncrement()));

        uni.subscribe().withSubscriber(s1);
        s1.assertFailure(IllegalStateException.class, "boom");
        uni.subscribe().withSubscriber(s2);
        s2.assertFailure(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testWithSharedStateProducingNull() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> null;

        Uni<Integer> uni = Uni.createFrom().deferred(boom,
                page -> Uni.createFrom().item(page.getAndIncrement()));

        uni.subscribe().withSubscriber(s1);
        s1.assertFailure(NullPointerException.class, "supplier");
        uni.subscribe().withSubscriber(s2);
        s2.assertFailure(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testThatStateSupplierCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().deferred(null,
                x -> Uni.createFrom().item("x")));
    }

    @Test
    public void testThatFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().deferred(() -> "hello",
                null));
    }

}
