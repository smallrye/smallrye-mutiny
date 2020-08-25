package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class DroppedExceptionsTest {

    private static final PrintStream systemErr = System.err;

    @After
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
                .startsWith("[-- Mutiny had to drop the following exception --]")
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
                .onCancellation().invokeUni(() -> Uni.createFrom().failure(new IOException("boom")))
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
                .onCancellation().invokeUni(() -> Uni.createFrom().failure(new IOException("boom")))
                .subscribe().with(item -> {
                    // Nothing to see here anyway
                });

        cancellable.cancel();
        assertThat(outputStream.toString())
                .startsWith("[-- Mutiny had to drop the following exception --]")
                .contains("boom")
                .contains("java.io.IOException")
                .contains("io.smallrye.mutiny.infrastructure.DroppedExceptionsTest")
                .contains("[------------------------------------------------]");
    }
}
