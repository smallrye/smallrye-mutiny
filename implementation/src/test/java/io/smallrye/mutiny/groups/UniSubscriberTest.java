package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
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

    @Test
    public void testSingleCallbackVariant() {
        AtomicInteger result = new AtomicInteger();
        Uni.createFrom().item(1).subscribe().with(result::set);
        assertThat(result).hasValue(1);

        AtomicReference<String> value = new AtomicReference<>("sentinel");
        Uni.createFrom().<String> nullItem().subscribe().with(value::set);
        assertThat(value).hasValue(null);

        result.set(-1);
        Uni.createFrom().<Integer> failure(new IOException("boom")).subscribe().with(result::set);
        assertThat(result).hasValue(-1);

        // Assert cancellation before the emission
        AtomicBoolean called = new AtomicBoolean();
        Cancellable cancellable = Uni.createFrom().nothing().subscribe().with(x -> called.set(true));
        assertThat(called).isFalse();
        cancellable.cancel();
        assertThat(called).isFalse();

        // Assert cancellation after the emission
        cancellable = Uni.createFrom().item(1).subscribe().with(x -> called.set(true));
        assertThat(called).isTrue();
        cancellable.cancel();
    }

    @Test
    public void testTwoCallbacksVariant() {
        AtomicInteger result = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Uni.createFrom().item(1).subscribe().with(result::set, failure::set);
        assertThat(result).hasValue(1);
        assertThat(failure).hasValue(null);

        AtomicReference<String> value = new AtomicReference<>("sentinel");
        Uni.createFrom().<String> nullItem().subscribe().with(value::set, failure::set);
        assertThat(value).hasValue(null);
        assertThat(failure).hasValue(null);

        result.set(-1);
        Uni.createFrom().<Integer> failure(new IOException("boom")).subscribe().with(result::set, failure::set);
        assertThat(result).hasValue(-1);
        assertThat(failure.get()).isInstanceOf(IOException.class).hasMessage("boom");

        // Assert cancellation before the emission
        AtomicBoolean called = new AtomicBoolean();
        Cancellable cancellable = Uni.createFrom().nothing().subscribe()
                .with(x -> called.set(true), f -> called.set(true));
        assertThat(called).isFalse();
        cancellable.cancel();
        assertThat(called).isFalse();

        // Assert cancellation after the emission
        cancellable = Uni.createFrom().item(1).subscribe()
                .with(x -> called.set(true), f -> called.set(true));
        assertThat(called).isTrue();
        cancellable.cancel();
    }

}
