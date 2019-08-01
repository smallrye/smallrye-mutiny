package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class UniOnEventTest {

    @Test
    public void testActionsOnResult() {
        AtomicInteger result = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicInteger terminate = new AtomicInteger();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().result(1)
                .onResult().consume(result::set)
                .onFailure().consume(failure::set)
                .on().subscription(subscription::set)
                .on().termination((r, f, c)  -> terminate.set(r))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

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
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicReference<Throwable> terminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer>failure(new IOException("boom"))
                .onResult().consume(result::set)
                .onFailure().consume(failure::set)
                .on().subscription(subscription::set)
                .on().termination((r, f, c)  -> terminate.set(f))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

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
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicInteger resultFromTerminate = new AtomicInteger();
        AtomicReference<Throwable> failureFromTerminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().result(1)
                .onResult().consume(i -> {
                    throw new IllegalStateException("boom");
                })
                .onFailure().consume(failure::set)
                .on().subscription(subscription::set)
                .on().termination((r, f, c)  -> {
                    if (r != null) {
                        resultFromTerminate.set(r);
                    }
                    failureFromTerminate.set(f);
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

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
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        AtomicInteger resultFromTerminate = new AtomicInteger();
        AtomicReference<Throwable> failureFromTerminate = new AtomicReference<>();
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().<Integer>failure(new IOException("kaboom"))
                .onResult().consume(result::set)
                .onFailure().consume(e -> {
                    throw new IllegalStateException("boom");
                })
                .on().subscription(subscription::set)
                .on().termination((r, f, c)  -> {
                    if (r != null) {
                        resultFromTerminate.set(r);
                    }
                    failureFromTerminate.set(f);
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(result).doesNotHaveValue(1);
        assertThat(subscription.get()).isNotNull();
        assertThat(resultFromTerminate).doesNotHaveValue(1);
        assertThat(failureFromTerminate.get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testWhenOnSubscriptionThrowsAnException() {
        UniAssertSubscriber<? super Integer> subscriber = Uni.createFrom().result(1)
                .on().subscription(s -> {
                    throw new IllegalStateException("boom");
                }).subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailure(IllegalStateException.class, "boom");
    }

    @Test
    public void testOnCancelWithImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber =
                Uni.createFrom().result(1)
                        .on().cancellation(() -> called.set(true))
                        .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotCompleted();
        assertThat(called).isTrue();
    }

    @Test
    public void testOnTerminationWithCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicBoolean terminated = new AtomicBoolean();
        UniAssertSubscriber<? super Integer> subscriber =
                Uni.createFrom().result(1)
                        .on().termination((r, f, c)  -> terminated.set(c))
                        .on().cancellation(() -> called.set(true))
                        .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotCompleted();
        assertThat(called).isTrue();
        assertThat(terminated).isTrue();
    }

}