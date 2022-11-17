package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class UniCreateFromItemTest {

    @Test
    public void testThatNullValueAreAccepted() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().item((String) null).subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(null);
    }

    @Test
    public void testWithNonNullValue() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().item(1).subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(1);
    }

    @Test
    public void testThatEmptyIsAcceptedWithFromOptional() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().optional(Optional.empty()).subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(null);
    }

    @SuppressWarnings({ "OptionalAssignedToNull", "unchecked", "rawtypes" })
    @Test
    public void testThatNullIfNotAcceptedByFromOptional() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().optional((Optional) null) // Immediate failure, no need for subscription
        );
    }

    @Test
    public void testThatFulfilledOptionalIsAcceptedWithFromOptional() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().optional(Optional.of(1)).subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(1);
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscription() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni<Integer> uni = Uni.createFrom().item(1).map(i -> {
            called.set(true);
            return i + 1;
        });

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(2);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatValueIsRetrievedUsingBlock() {
        assertThat(Uni.createFrom().item("foo").await().indefinitely()).isEqualToIgnoringCase("foo");
    }

    @Test
    public void testWithImmediateCancellation() {
        UniAssertSubscriber<String> subscriber1 = new UniAssertSubscriber<>(true);
        UniAssertSubscriber<String> subscriber2 = new UniAssertSubscriber<>(false);
        Uni<String> foo = Uni.createFrom().item("foo");
        foo.subscribe().withSubscriber(subscriber1);
        foo.subscribe().withSubscriber(subscriber2);
        subscriber1.assertNotTerminated();
        subscriber2.assertCompleted().assertItem("foo");
    }

    @Test
    public void testEmpty() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().voidItem().subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(null);
    }

    @Test
    public void testEmptyTyped() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<String> nullItem().subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(null);
    }

    @Test
    public void testEmptyWithImmediateCancellation() {
        UniAssertSubscriber<Void> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().voidItem().subscribe().withSubscriber(subscriber);
        subscriber.assertNotTerminated();
    }

    @Test
    public void testEmptyTypedWithImmediateCancellation() {
        UniAssertSubscriber<String> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().<String> nullItem().subscribe().withSubscriber(subscriber);
        subscriber.assertNotTerminated();
    }

    @Test
    public void testWithSharedState() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        AtomicInteger shared = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().item(() -> shared,
                AtomicInteger::incrementAndGet);

        assertThat(shared).hasValue(0);
        uni.subscribe().withSubscriber(s1);
        assertThat(shared).hasValue(1);
        s1.assertCompleted().assertItem(1);
        uni.subscribe().withSubscriber(s2);
        assertThat(shared).hasValue(2);
        s2.assertCompleted().assertItem(2);
    }

    @Test
    public void testWithSharedStateProducingFailure() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> {
            throw new IllegalStateException("boom");
        };

        Uni<Integer> uni = Uni.createFrom().item(boom,
                AtomicInteger::incrementAndGet);

        uni.subscribe().withSubscriber(s1);
        s1.assertFailedWith(IllegalStateException.class, "boom");
        uni.subscribe().withSubscriber(s2);
        s2.assertFailedWith(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testWithSharedStateProducingNull() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> null;

        Uni<Integer> uni = Uni.createFrom().item(boom,
                AtomicInteger::incrementAndGet);

        uni.subscribe().withSubscriber(s1);
        s1.assertFailedWith(NullPointerException.class, "supplier");
        uni.subscribe().withSubscriber(s2);
        s2.assertFailedWith(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testThatStateSupplierCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(null,
                x -> "x"));
    }

    @Test
    public void testThatFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(() -> "hello",
                null));
    }

}
