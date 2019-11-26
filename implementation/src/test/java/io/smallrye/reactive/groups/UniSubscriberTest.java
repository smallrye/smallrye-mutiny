package io.smallrye.reactive.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.smallrye.reactive.Uni;

public class UniSubscriberTest {
    @Test
    public void testCancelSubscription() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().item(null).onItem().delayIt().by(Duration.ofMillis(50))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set).cancel();

        await().until(() -> counter.intValue() == 0);
        assertThat(failure.get()).isNull();
    }

    @Test
    public void testFailure() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().failure(new Throwable("Cause failure"))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 0);
        assertThat(failure.get()).isNotNull();
    }

    @Test
    public void testSubscriptionRequestIgnored() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().item(null).onItem().delayIt().by(Duration.ofMillis(50))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set).request(5);

        await().until(() -> counter.intValue() == 1);
        assertThat(failure.get()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubscriptionRequestException() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().item(null).onItem().delayIt().by(Duration.ofMillis(50))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set).request(0);
    }
}
