package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.test.AssertSubscriber;

@SuppressWarnings("ConstantConditions")
public class MultiOnFailureInvokeTest {

    public static final IOException BOOM = new IOException("boom");
    private final Multi<Integer> numbers = Multi.createFrom().items(1, 2);
    private final Multi<Integer> failed = Multi.createBy().concatenating()
            .streams(numbers, Multi.createFrom().failure(BOOM));

    @Test
    public void testInvokeWhenUpstreamEmitAnItem() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger twoGotCalled = new AtomicInteger();

        AssertSubscriber<Integer> subscriber = numbers.onFailure().invoke(failure::set)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(1, 2);
        assertThat(twoGotCalled).hasValue(0);
        assertThat(failure).hasValue(null);
    }

    @Test
    public void testInvokeWithRunnableWhenUpstreamEmitAnItem() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicInteger twoGotCalled = new AtomicInteger();

        AssertSubscriber<Integer> subscriber = numbers.onFailure().invoke(() -> called.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(1, 2);
        assertThat(twoGotCalled).hasValue(0);
        assertThat(called).isFalse();
    }

    @Test
    public void testInvokeWhenUpstreamEmitAFailure() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = failed.onFailure().invoke(failure::set)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testInvokeWithRunnableWhenUpstreamEmitAFailure() {
        AtomicBoolean called = new AtomicBoolean();

        AssertSubscriber<Integer> subscriber = failed.onFailure().invoke(() -> called.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 2);
        assertThat(called).isTrue();
    }

    @Test
    public void testFailureInCallback() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = failed.onFailure().invoke(t -> {
            failure.set(t);
            throw new RuntimeException("kaboom");
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "kaboom")
                .assertReceived(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testFailureInCallbackWithRunnable() {
        AssertSubscriber<Integer> subscriber = failed.onFailure().invoke(() -> {
            throw new RuntimeException("kaboom");
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "kaboom")
                .assertReceived(1, 2);
    }

    @RepeatedTest(10)
    public void testCancellationBeforeActionCompletes() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean called = new AtomicBoolean();

        AssertSubscriber<Integer> subscriber = failed
                .emitOn(Infrastructure.getDefaultExecutor())
                .onFailure().invoke(i -> {
                    try {
                        called.set(true);
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        await().until(called::get);
        subscriber.cancel();
        latch.countDown();
    }

    @Test
    public void testImmediateCancellation() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean called = new AtomicBoolean();

        AssertSubscriber<Integer> subscriber = failed
                .emitOn(Infrastructure.getDefaultExecutor())
                .onFailure().invoke(i -> {
                    try {
                        called.set(true);
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .subscribe().withSubscriber(new AssertSubscriber<>(10, true));

        latch.countDown();
        subscriber.assertNotTerminated();
        assertThat(called).isFalse();
    }
}
