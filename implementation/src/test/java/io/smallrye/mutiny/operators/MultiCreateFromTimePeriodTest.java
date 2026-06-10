package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiCreateFromTimePeriodTest {

    private ScheduledExecutorService executor;

    @BeforeEach
    public void prepare() {
        executor = Executors.newScheduledThreadPool(1);
    }

    @AfterEach
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    public void testIntervalOfAFewMillis() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        // Add a fake item with the beginning time
        subscriber.getItems().add(System.currentTimeMillis());

        Multi.createFrom().ticks()
                .startingAfter(Duration.ofMillis(100)).onExecutor(executor).every(Duration.ofMillis(100))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitNextItems(10)
                .cancel();

        subscriber.assertNotTerminated();

        List<Long> list = subscriber.getItems();
        for (int i = 0; i < list.size() - 1; i++) {
            long delta = list.get(i + 1) - list.get(i);
            assertThat(delta).isBetween(20L, 350L);
        }
    }

    @Test
    public void testThatTicksStartAfterRequest() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create();

        Multi.createFrom().ticks()
                .every(Duration.ofMillis(100))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(subscriber);

        await().pollDelay(Duration.ofMillis(500)).untilAsserted(subscriber::assertNotTerminated);

        subscriber.request(100);
        subscriber.awaitNextItems(10)
                .cancel();

        subscriber.assertNotTerminated();
    }

    @Test
    public void testWithInfraExecutorAndNoDelay() throws InterruptedException {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        // Add a fake item with the beginning time
        subscriber.getItems().add(System.currentTimeMillis());

        // No initial delay, so introduce a fake delay
        Thread.sleep(100);

        Multi.createFrom().ticks()
                .every(Duration.ofMillis(100))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitNextItems(10)
                .cancel();

        subscriber.assertNotTerminated();

        List<Long> list = subscriber.getItems();
        for (int i = 0; i < list.size() - 1; i++) {
            long delta = list.get(i + 1) - list.get(i);
            assertThat(delta).isBetween(20L, 350L);
        }
    }

    @Test
    public void testBackPressureOverflow() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create();

        subscriber.getItems().add(System.currentTimeMillis());

        Multi.createFrom().ticks()
                .startingAfter(Duration.ofMillis(50)).onExecutor(executor).every(Duration.ofMillis(50))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(subscriber);

        subscriber
                .request(2) // request only 2
                .awaitFailure(t -> { // wait until failure, and validate
                    assertThat(t)
                            .isInstanceOf(BackPressureFailure.class)
                            .hasMessageContaining("lack of requests");
                });
    }

    @RepeatedTest(5)
    public void testConcurrentRequestDoesNotDoubleStart() throws Exception {
        int threadCount = 8;
        AtomicInteger taskCount = new AtomicInteger();
        AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        ScheduledExecutorService spyExecutor = new ScheduledThreadPoolExecutor(4) {
            @Override
            public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                    TimeUnit unit) {
                taskCount.incrementAndGet();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.scheduleAtFixedRate(command, initialDelay, period, unit);
            }
        };

        try {
            Multi<Long> ticks = Multi.createFrom().ticks()
                    .onExecutor(spyExecutor).every(Duration.ofMillis(100));

            ticks.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriptionRef.set(subscription);
                }

                @Override
                public void onNext(Long item) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }
            });

            await().atMost(Duration.ofSeconds(5)).until(() -> subscriptionRef.get() != null);
            Flow.Subscription subscription = subscriptionRef.get();

            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                    }
                    subscription.request(1);
                });
            }
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);

            subscription.cancel();

            assertThat(taskCount.get())
                    .as("scheduleAtFixedRate should be called exactly once")
                    .isEqualTo(1);
        } finally {
            spyExecutor.shutdownNow();
        }
    }

}
