package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiCreateFromTimePeriodTest {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    public void testIntervalOfAFewMillis() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        // Add a fake item with the beginning time
        subscriber.items().add(System.currentTimeMillis());

        Multi.createFrom().ticks()
                .startingAfter(Duration.ofMillis(100)).onExecutor(executor).every(Duration.ofMillis(100))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(subscriber);

        await().until(() -> subscriber.items().size() >= 10);
        subscriber.cancel();

        subscriber
                .assertHasNotCompleted()
                .assertHasNotFailed();

        List<Long> list = subscriber.items();
        for (int i = 0; i < list.size() - 1; i++) {
            long delta = list.get(i + 1) - list.get(i);
            assertThat(delta).isBetween(20L, 350L);
        }
    }

    @Test
    public void testWithInfraExecutorAndNoDelay() throws InterruptedException {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        // Add a fake item with the beginning time
        subscriber.items().add(System.currentTimeMillis());

        // No initial delay, so introduce a fake delay
        Thread.sleep(100);

        Multi.createFrom().ticks()
                .every(Duration.ofMillis(100))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(subscriber);

        await().until(() -> subscriber.items().size() >= 10);
        subscriber.cancel();

        subscriber
                .assertHasNotCompleted()
                .assertHasNotFailed();

        List<Long> list = subscriber.items();
        for (int i = 0; i < list.size() - 1; i++) {
            long delta = list.get(i + 1) - list.get(i);
            assertThat(delta).isBetween(20L, 350L);
        }
    }

    @Test
    @Timeout(1)
    public void testBackPressureOverflow() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create();

        subscriber.items().add(System.currentTimeMillis());

        Multi.createFrom().ticks()
                .startingAfter(Duration.ofMillis(50)).onExecutor(executor).every(Duration.ofMillis(50))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(subscriber);

        subscriber
                .request(2) // request only 2
                .await() // wait until failure
                .assertHasFailedWith(BackPressureFailure.class, "lack of requests");
    }

}
