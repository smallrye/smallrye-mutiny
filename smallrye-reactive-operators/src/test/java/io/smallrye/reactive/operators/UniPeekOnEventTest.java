package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniSubscription;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class UniPeekOnEventTest {

    @Test
    public void testActionsOnResult() {
        AtomicInteger result = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<UniSubscription> subscription = new AtomicReference<>();
        AtomicInteger terminate = new AtomicInteger();
        AssertSubscriber<? super Integer> subscriber = Uni.createFrom().result(1)
                .onResult().peek(result::set)
                .onFailure().peek(failure::set)
                .onSubscription(subscription::set)
                .onTerminate((i, f) -> terminate.set(i))
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertResult(1);
        assertThat(result).hasValue(1);
        assertThat(failure.get()).isNull();
        assertThat(subscription.get()).isNotNull();
        assertThat(terminate).hasValue(1);
    }


    @Test
    public void testActionsOnFailures() {
        AtomicInteger result = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<UniSubscription> subscription = new AtomicReference<>();
        AtomicReference<Throwable> terminate = new AtomicReference<>();
        AssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer>failure(new IOException("boom"))
                .onResult().peek(result::set)
                .onFailure().peek(failure::set)
                .onSubscription(subscription::set)
                .onTerminate((i, f) -> terminate.set(f))
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IOException.class, "boom");
        assertThat(result).doesNotHaveValue(1);
        assertThat(failure.get()).isInstanceOf(IOException.class);
        assertThat(subscription.get()).isNotNull();
        assertThat(terminate.get()).isInstanceOf(IOException.class);
    }

    @Test
    public void testWhenOnResultThrowsAnException() {
        AtomicInteger result = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<UniSubscription> subscription = new AtomicReference<>();
        AtomicInteger resultFromTerminate = new AtomicInteger();
        AtomicReference<Throwable> failureFromTerminate = new AtomicReference<>();
        AssertSubscriber<? super Integer> subscriber = Uni.createFrom().result(1)
                .onResult().peek(i -> {
                    throw new IllegalStateException("boom");
                })
                .onFailure().peek(failure::set)
                .onSubscription(subscription::set)
                .onTerminate((i, f) -> {
                    if (i != null) {
                        resultFromTerminate.set(i);
                    }
                    failureFromTerminate.set(f);
                })
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(result).doesNotHaveValue(1);
        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
        assertThat(subscription.get()).isNotNull();
        assertThat(resultFromTerminate).doesNotHaveValue(1);
        assertThat(failureFromTerminate.get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testWhenOnFailureThrowsAnException() {
        AtomicInteger result = new AtomicInteger();
        AtomicReference<UniSubscription> subscription = new AtomicReference<>();
        AtomicInteger resultFromTerminate = new AtomicInteger();
        AtomicReference<Throwable> failureFromTerminate = new AtomicReference<>();
        AssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer>failure(new IOException("kaboom"))
                .onResult().peek(result::set)
                .onFailure().peek(e -> {
                    throw new IllegalStateException("boom");
                })
                .onSubscription(subscription::set)
                .onTerminate((i, f) -> {
                    if (i != null) {
                        resultFromTerminate.set(i);
                    }
                    failureFromTerminate.set(f);
                })
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(result).doesNotHaveValue(1);
        assertThat(subscription.get()).isNotNull();
        assertThat(resultFromTerminate).doesNotHaveValue(1);
        assertThat(failureFromTerminate.get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testWhenOnSubscriptionThrowsAnException() {
        AssertSubscriber<? super Integer> subscriber = Uni.createFrom().result(1).onSubscription(s -> {
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertFailure(IllegalStateException.class, "boom");
    }

    @Test
    public void testOnCancelWithImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AssertSubscriber<? super Integer> subscriber =
                Uni.createFrom().result(1)
                        .onCancellation(() -> called.set(true))
                        .subscribe().withSubscriber(new AssertSubscriber<>(true));

        subscriber.assertNotCompleted();
        assertThat(called).isTrue();
    }

}