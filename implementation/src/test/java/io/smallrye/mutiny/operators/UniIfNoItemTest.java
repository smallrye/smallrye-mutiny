package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.testng.annotations.Test;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

public class UniIfNoItemTest {

    @Test
    public void testResultWhenTimeoutIsNotReached() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithUni(Uni.createFrom().nothing())
                .subscribe().withSubscriber(subscriber);

        subscriber.await().assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testTimeout() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .onItem().delayIt().by(Duration.ofMillis(10))
                .ifNoItem().after(Duration.ofMillis(1)).fail()
                .subscribe().withSubscriber(subscriber);

        subscriber.await().assertCompletedWithFailure();
        assertThat(subscriber.getFailure()).isInstanceOf(TimeoutException.class);

    }

    @Test
    public void testRecoverWithItem() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithItem(5)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertItem(5);
    }

    @Test
    public void testRecoverWithItemSupplier() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithItem(() -> 23)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertItem(23);
    }

    @Test
    public void testRecoverWithSwitchToUni() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithUni(() -> Uni.createFrom().item(15))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertItem(15);
    }

    @Test
    public void testFailingWithAnotherException() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).failWith(new IOException("boom"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.await().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testDurationValidity() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).ifNoItem().after(null))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).ifNoItem().after(Duration.ofMillis(0)))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).ifNoItem().after(Duration.ofMillis(-1)))
                .withMessageContaining("timeout");
    }

    @Test
    public void testFailingOnTimeoutWithShutdownExecutor() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.shutdown();
        UniAssertSubscriber<Object> subscriber = Uni.createFrom().emitter(e -> {
            // To nothing
        })
                .ifNoItem().after(Duration.ofMillis(10)).on(executor).fail()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .await()
                .assertCompletedWithFailure()
                .assertFailure(RejectedExecutionException.class, "");
    }

    @Test
    public void testFailingOnTimeoutWithImmediateCancellation() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        UniAssertSubscriber<Object> subscriber = Uni.createFrom().emitter(e -> {
            // To nothing
        })
                .ifNoItem().after(Duration.ofMillis(10)).on(executor).fail()
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotCompleted()
                .assertSubscribed()
                .assertNoResult();
    }

    @Test
    public void testFailingOnTimeoutWithCancellation() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        UniAssertSubscriber<Object> subscriber = Uni.createFrom().emitter(e -> {
            // To nothing
        })
                .ifNoItem().after(Duration.ofMillis(1000)).on(executor).fail()
                .subscribe().withSubscriber(new UniAssertSubscriber<>(false));

        subscriber.cancel();

        subscriber.assertNotCompleted()
                .assertSubscribed()
                .assertNoResult();
    }

}
