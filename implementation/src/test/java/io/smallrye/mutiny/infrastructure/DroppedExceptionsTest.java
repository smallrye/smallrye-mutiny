package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Test;

public class DroppedExceptionsTest {

    private static final PrintStream systemErr = System.err;

    @After
    public void cleanup() {
        System.setErr(systemErr);
        Infrastructure.resetDroppedExceptionHandler();
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
}
