package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniSubscriberTest {
    @Test
    public void testCancelSubscription() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().item((Object) null).onItem().delayIt().by(Duration.ofMillis(50))
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
}
