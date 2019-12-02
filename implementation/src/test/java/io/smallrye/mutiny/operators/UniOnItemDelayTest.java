package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOnItemDelayTest {

    private ScheduledExecutorService executor;

    private Uni<Void> delayed;

    @BeforeMethod
    public void init() {
        executor = Executors.newScheduledThreadPool(4);
        delayed = Uni.createFrom().item((Void) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100));
    }

    @AfterMethod
    public void shutdown() {
        executor.shutdown();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullDuration() {
        Uni.createFrom().item(1).onItem().delayIt().by(null);
    }

    @Test
    public void testDelayOnResultWithDefaultExecutor() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();

        Uni.createFrom().item(null)
                .onItem().castTo(Void.class)
                .onItem().delayIt()
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);

        subscriber.await();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isGreaterThanOrEqualTo(100);
        subscriber.assertCompletedSuccessfully().assertItem(null);
        assertThat(subscriber.getOnResultThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNegativeDuration() {
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofDays(-1));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithZeroAsDuration() {
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ZERO);
    }

    @Test
    public void testDelayOnResult() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        delayed.subscribe().withSubscriber(subscriber);
        subscriber.await();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isGreaterThanOrEqualTo(100);
        subscriber.assertCompletedSuccessfully().assertItem(null);
        assertThat(subscriber.getOnResultThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testThatDelayDoNotImpactFailures() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Void> failure(new Exception("boom")).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);
        subscriber.await();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isLessThan(100);
        subscriber.assertCompletedWithFailure().assertFailure(Exception.class, "boom");
    }

    @Test
    public void testThatNothingIsSubmittedOnImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        executor.shutdown();
        executor = new ScheduledThreadPoolExecutor(4) {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                called.set(true);
                return super.schedule(command, delay, unit);
            }
        };

        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().item(1).onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(100)).subscribe()
                .withSubscriber(subscriber);
        subscriber.assertNotCompleted();
        assertThat(called).isFalse();
    }

    @Test
    public void testRejectedScheduling() {
        executor.shutdown();
        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedWithFailure().assertFailure(RejectedExecutionException.class, "");
    }

    @Test
    public void testCancellationHappeningDuringTheWaitingTime() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();
        executor.shutdown();

        executor = new ScheduledThreadPoolExecutor(4) {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                ScheduledFuture<?> schedule = super.schedule(() -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    command.run();

                }, delay, unit);
                future.set(schedule);
                return schedule;
            }
        };

        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);
        subscriber.cancel();
        latch.countDown();

        await().until(() -> future.get() != null && future.get().isCancelled());
        subscriber.assertNotCompleted();
    }

    @Test
    public void testWithMultipleDelays() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Uni.createFrom().item(null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(50))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        Uni.createFrom().item(null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(200))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);
        Uni.createFrom().item(null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(400))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);
        Uni.createFrom().item(null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(800)).subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 4);
        assertThat(failure.get()).isNull();

    }
}
