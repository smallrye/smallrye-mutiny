package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
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

        subscriber.assertCompleted()
                .assertItems(1, 2);
        assertThat(twoGotCalled).hasValue(0);
        assertThat(failure).hasValue(null);
    }

    @Test
    public void testInvokeWithRunnableWhenUpstreamEmitAnItem() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicInteger twoGotCalled = new AtomicInteger();

        AssertSubscriber<Integer> subscriber = numbers.onFailure().invoke(() -> called.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertCompleted()
                .assertItems(1, 2);
        assertThat(twoGotCalled).hasValue(0);
        assertThat(called).isFalse();
    }

    @Test
    public void testInvokeWhenUpstreamEmitAFailure() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = failed.onFailure().invoke(failure::set)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertFailedWith(IOException.class, "boom")
                .assertItems(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testInvokeWithRunnableWhenUpstreamEmitAFailure() {
        AtomicBoolean called = new AtomicBoolean();

        AssertSubscriber<Integer> subscriber = failed.onFailure().invoke(() -> called.set(true))
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertFailedWith(IOException.class, "boom")
                .assertItems(1, 2);
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
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "kaboom")
                .assertItems(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testFailureInCallbackWithRunnable() {
        AssertSubscriber<Integer> subscriber = failed.onFailure().invoke(() -> {
            throw new RuntimeException("kaboom");
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "kaboom")
                .assertItems(1, 2);
    }

    @RepeatedTest(10)
    public void testCancellationBeforeActionCompletes() {
        AtomicBoolean onFailureEntered = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();

        ExecutorService executor = Executors.newFixedThreadPool(1);
        AssertSubscriber<Integer> subscriber = failed
                .emitOn(executor)
                .onFailure().invoke(i -> {
                    onFailureEntered.set(true);
                    await().atMost(5, TimeUnit.SECONDS).and().untilTrue(cancelled);
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        with().pollDelay(100, TimeUnit.MILLISECONDS).await().atMost(5, TimeUnit.SECONDS).and().untilTrue(onFailureEntered);
        subscriber.cancel();
        cancelled.set(true);
        executor.shutdownNow();
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
