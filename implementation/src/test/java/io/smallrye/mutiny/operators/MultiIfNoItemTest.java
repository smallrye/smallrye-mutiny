package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiIfNoItemTest {

    @Test
    public void testResultWhenTimeoutIsNotReached() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(10);

        Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .select().first(4)
                .ifNoItem().after(Duration.ofMillis(20)).recoverWithCompletion()
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitCompletion().assertItems(0L, 1L, 2L, 3L);
    }

    @Test
    public void testTimeoutBeforeFirstItem() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(10);

        Multi.createFrom().ticks().startingAfter(Duration.ofMillis(20)).every(Duration.ofMillis(20))
                .ifNoItem().after(Duration.ofMillis(10)).fail()
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitFailure()
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(TimeoutException.class);
    }

    @Test
    public void testTimeoutAfterFirstItem() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(10);

        Multi.createFrom().ticks().every(Duration.ofMillis(20))
                .ifNoItem().after(Duration.ofMillis(10)).fail()
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitFailure()
                .assertItems(0L)
                .assertFailedWith(TimeoutException.class);
    }

    @Test
    public void testTimeoutOnFailure() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Long> emitter(e -> new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            e.fail(new TestException("boom"));
        }).start())
                .ifNoItem().after(Duration.ofMillis(1)).fail()
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitFailure()
                .assertFailedWith(TimeoutException.class);
    }

    @Test
    public void testFailureBeforeTimeout() {
        AtomicBoolean subscribed = new AtomicBoolean(false);
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> emitter(e -> new Thread(() -> {
            await().untilTrue(subscribed);
            e.fail(new TestException("boom"));
        }).start())
                .ifNoItem().after(Duration.ofMillis(10000)).fail()
                .subscribe().withSubscriber(subscriber);

        subscribed.set(true);
        subscriber.awaitFailure()
                .assertFailedWith(TestException.class);
    }

    @Test
    public void testRecoverWithCompletion() {
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithCompletion()
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.awaitCompletion();
    }

    @Test
    public void testRecoverWithSwitchToMulti() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithMulti(Multi.createFrom().item(15))
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        subscriber.awaitCompletion().assertItems(15);
    }

    @Test
    public void testRecoverWithSwitchToMultiSupplier() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithMulti(() -> Multi.createFrom().item(15))
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        subscriber.awaitCompletion().assertItems(15);
    }

    @Test
    public void testFailingWithAnotherException() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).failWith(new IOException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.awaitFailure().assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDurationValidity() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createFrom().item(1).ifNoItem().after(null))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createFrom().item(1).ifNoItem().after(Duration.ofMillis(0)))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createFrom().item(1).ifNoItem().after(Duration.ofMillis(-1)))
                .withMessageContaining("timeout");
    }

    @Test
    public void testFailingOnTimeoutWithShutdownExecutor() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.shutdown();
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(10)).on(executor).fail()
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber
                .awaitFailure()
                .assertFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testFailingOnItemWithShutdownExecutor() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        AssertSubscriber<Object> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .ifNoItem().after(Duration.ofMillis(20)).on(executor).fail()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        executor.shutdown();

        subscriber
                .awaitFailure()
                .assertFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testFailingOnTimeoutWithImmediateCancellation() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(10)).on(executor).fail()
                .subscribe().withSubscriber(new AssertSubscriber<>(1, true));

        subscriber.assertNotTerminated()
                .assertSubscribed();
    }

    @Test
    public void testFailingOnTimeoutWithCancellation() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(1000)).on(executor).fail()
                .subscribe().withSubscriber(new AssertSubscriber<>(1, false));

        subscriber.cancel();

        subscriber.assertNotTerminated()
                .assertSubscribed();
    }

    @Test
    public void testTimeoutWithCustomSupplierFailing() {
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(1)).failWith(() -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.awaitFailure()
                .assertFailedWith(IllegalArgumentException.class);
    }

    @Test
    public void testTimeoutWithCustomSupplierReturningNull() {
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(1)).failWith(() -> null)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.awaitFailure()
                .assertFailedWith(NullPointerException.class, ParameterValidation.SUPPLIER_PRODUCED_NULL);
    }
}
