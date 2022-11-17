package io.smallrye.mutiny.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class CallbackBasedSubscriberTest {

    @AfterEach
    public void cleanup() {
        Infrastructure.resetDroppedExceptionHandler();
    }

    @Test
    public void testOnSubscriberThrowingException() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);
        Multi.createFrom().item(1)
                .onCancellation().invoke(() -> cancelled.set(true))
                .subscribe().with(
                        sub -> {
                            throw new IllegalArgumentException("boom");
                        },
                        i -> {
                        },
                        failure::set,
                        () -> completed.set(true));

        assertThat(completed).isFalse();
        assertThat(failure.get()).isNull();
        assertThat(cancelled).isTrue();
        assertThat(captured.get()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("boom");
    }

    @Test
    public void testOnItemThrowingException() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);
        Multi.createFrom().item(1)
                .onCancellation().invoke(() -> cancelled.set(true))
                .subscribe().with(
                        i -> {
                            throw new IllegalArgumentException("boom");
                        },
                        failure::set,
                        () -> completed.set(true));

        assertThat(completed).isFalse();
        assertThat(failure.get()).isNull();
        assertThat(cancelled).isTrue();
        assertThat(captured.get()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("boom");
    }

    @Test
    public void testOnFailureThrowingException() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);
        Multi.createFrom().failure(new IOException("I/O"))
                .onCancellation().invoke(() -> cancelled.set(true))
                .subscribe().with(
                        i -> {
                        },
                        f -> {
                            throw new IllegalArgumentException("boom");
                        },
                        () -> completed.set(true));

        assertThat(completed).isFalse();
        assertThat(cancelled).isFalse();
        assertThat(captured.get()).isInstanceOf(CompositeException.class)
                .hasMessageContaining("boom").hasMessageContaining("I/O");
    }

    @Test
    public void testOnFailureAfterCancellation() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);
        Multi.createFrom().failure(new IOException("I/O"))
                .onCancellation().invoke(() -> cancelled.set(true))
                .subscribe().with(
                        Subscription::cancel,
                        i -> {
                        },
                        f -> {
                            throw new IllegalArgumentException("boom");
                        },
                        () -> completed.set(true));

        assertThat(completed).isFalse();
        assertThat(cancelled).isTrue();
        assertThat(captured.get()).isInstanceOf(IOException.class)
                .hasMessageContaining("I/O");
    }

    @Test
    public void testDroppedFailureWithoutFailureCallback() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);
        Multi.createFrom().failure(new IOException("I/O"))
                .onCancellation().invoke(() -> cancelled.set(true))
                .subscribe().withSubscriber(new Subscribers.CallbackBasedSubscriber<>(Context.empty(), i -> {
                },
                        null, null, s -> {
                        }));

        assertThat(completed).isFalse();
        assertThat(cancelled).isFalse();
        assertThat(captured.get()).isInstanceOf(IOException.class)
                .hasMessageContaining("I/O");
    }

}
