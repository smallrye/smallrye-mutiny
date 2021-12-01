package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class UniOnItemDelayTest {

    private ScheduledExecutorService executor;

    private Uni<Void> delayed;

    @BeforeEach
    public void init() {
        executor = Executors.newScheduledThreadPool(4);
        delayed = Uni.createFrom().voidItem().onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100));
    }

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void testWithNullDuration() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).onItem().delayIt().by(null));
    }

    @Test
    public void testDelayOnItemWithDefaultExecutor() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();

        Uni.createFrom().item((Object) null)
                .onItem().castTo(Void.class)
                .onItem().delayIt()
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);

        subscriber.awaitItem();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isGreaterThanOrEqualTo(100);
        subscriber.assertCompleted().assertItem(null);
        assertThat(subscriber.getOnItemThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofDays(-1)));
    }

    @Test
    public void testWithZeroAsDuration() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ZERO));
    }

    @Test
    public void testDelayOnItem() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        delayed.subscribe().withSubscriber(subscriber);
        subscriber.awaitItem();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isGreaterThanOrEqualTo(100);
        subscriber.assertCompleted().assertItem(null);
        assertThat(subscriber.getOnItemThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testThatDelayDoNotImpactFailures() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Void> failure(new Exception("boom")).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);
        subscriber.awaitFailure();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isLessThan(100);
        subscriber.assertFailedWith(Exception.class, "boom");
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
        subscriber.assertNotTerminated();
        assertThat(called).isFalse();
    }

    @Test
    public void testRejectedScheduling() {
        executor.shutdown();
        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);
        subscriber.assertFailed().assertFailedWith(RejectedExecutionException.class, "");
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
        subscriber.assertNotTerminated();
    }

    @Test
    public void testWithMultipleDelays() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Uni.createFrom().item((Object) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(50))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        Uni.createFrom().item((Object) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(200))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);
        Uni.createFrom().item((Object) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(400))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);
        Uni.createFrom().item((Object) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(800)).subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 4);
        assertThat(failure.get()).isNull();

    }
}
