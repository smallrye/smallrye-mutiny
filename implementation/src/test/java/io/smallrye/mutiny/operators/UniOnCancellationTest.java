package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class UniOnCancellationTest {

    @AfterEach
    void cleanup() {
        Infrastructure.resetDroppedExceptionHandler();
    }

    @Test
    void onCancellationCallbackThrowsInCancel() {
        AtomicBoolean upstreamCancelled = new AtomicBoolean(false);
        AtomicReference<Throwable> droppedException = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(droppedException::set);

        UniAssertSubscriber<String> subscriber = Uni.createFrom().<String> emitter(emitter -> {
        })
                .onCancellation().invoke(() -> upstreamCancelled.set(true))
                .onCancellation().invoke(() -> {
                    throw new RuntimeException("cancel callback failed");
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.cancel();

        assertThat(upstreamCancelled).isTrue();
        assertThat(droppedException.get()).isInstanceOf(RuntimeException.class).hasMessage("cancel callback failed");
    }
}
