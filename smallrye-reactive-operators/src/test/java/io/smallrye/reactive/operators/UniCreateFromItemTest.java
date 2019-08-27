package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class UniCreateFromItemTest {

    @Test
    public void testThatNullValueAreAccepted() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();
        Uni.createFrom().item((String) null).subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testWithNonNullValue() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().item(1).subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testThatEmptyIsAcceptedWithFromOptional() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();
        Uni.createFrom().optional(Optional.empty()).subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(null);
    }

    @SuppressWarnings({ "OptionalAssignedToNull", "unchecked" })
    @Test(expected = IllegalArgumentException.class)
    public void testThatNullIfNotAcceptedByFromOptional() {
        Uni.createFrom().optional((Optional) null); // Immediate failure, no need for subscription
    }

    @Test
    public void testThatFulfilledOptionalIsAcceptedWithFromOptional() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().optional(Optional.of(1)).subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscription() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni<Integer> uni = Uni.createFrom().item(1).map(i -> {
            called.set(true);
            return i + 1;
        });

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(2);
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
        subscriber1.assertNoResult().assertNoFailure();
        subscriber2.assertCompletedSuccessfully().assertItem("foo");
    }

    @Test
    public void testEmpty() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().nullItem().subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testEmptyWithImmediateCancellation() {
        UniAssertSubscriber<Void> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().nullItem().subscribe().withSubscriber(subscriber);
        subscriber.assertNoFailure().assertNoResult();
    }

}