package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

}
