package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class DroppedExceptionsTest {

    private static final PrintStream systemErr = System.err;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        System.setErr(systemErr);
        Infrastructure.resetDroppedExceptionHandler();
    }

    @Test
    public void rejectNullConsumer() {
        assertThatThrownBy(() -> Infrastructure.setDroppedExceptionHandler(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("handler");
    }

    @Test
    public void testDefaultHandler() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setErr(printStream);

        Infrastructure.handleDroppedException(new RuntimeException("yolo"));
        assertThat(outputStream.toString())
                .contains("[-- Mutiny had to drop the following exception --]")
                .contains("yolo")
                .contains("java.lang.RuntimeException")
                .contains("io.smallrye.mutiny.infrastructure.DroppedExceptionsTest.testDefaultHandler")
                .contains("[------------------------------------------------]");
    }

    @Test
    public void testCustomHandler() {
        AtomicReference<Throwable> box = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(box::set);
        Infrastructure.handleDroppedException(new RuntimeException("yolo"));
        assertThat(box.get()).isInstanceOf(RuntimeException.class).hasMessage("yolo");
    }

    @Test
    public void testCaptureOfDroppedExeption() {
        AtomicReference<Throwable> box = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(box::set);

        Cancellable cancellable = Uni.createFrom()
                .emitter(e -> {
                    // Never emit anything
                })
                .onCancellation().call(() -> Uni.createFrom().failure(new IOException("boom")))
                .subscribe().with(item -> {
                    // Nothing to see here anyway
                });

        cancellable.cancel();
        assertThat(box.get()).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    public void testCaptureOfDroppedExeptionWithDefaultHandler() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setErr(printStream);

        Cancellable cancellable = Uni.createFrom()
                .emitter(e -> {
                    // Never emit anything
                })
                .onCancellation().call(() -> Uni.createFrom().failure(new IOException("boom")))
                .subscribe().with(item -> {
                    // Nothing to see here anyway
                });

        cancellable.cancel();
        assertThat(outputStream.toString())
                .contains("[-- Mutiny had to drop the following exception --]")
                .contains("boom")
                .contains("java.io.IOException")
                .contains("io.smallrye.mutiny.infrastructure.DroppedExceptionsTest")
                .contains("[------------------------------------------------]");
    }
}
