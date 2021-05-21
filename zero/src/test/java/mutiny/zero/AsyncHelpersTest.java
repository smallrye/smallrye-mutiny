package mutiny.zero;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AsyncHelpersTest {

    @Test
    @DisplayName("Create a failed future")
    void failedFuture() {
        AtomicReference<Object> value = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        CompletableFuture<?> future = AsyncHelpers.failedFuture(new IOException("boom")).toCompletableFuture();
        assertTrue(future.isCompletedExceptionally());
        future.whenComplete((v, err) -> {
            value.set(v);
            error.set(err);
        });

        assertNull(value.get());
        assertNotNull(error.get());
        assertTrue(error.get() instanceof IOException);
        assertEquals("boom", error.get().getMessage());
    }

    @Nested
    @DisplayName("Map exceptions with applyExceptionally")
    class ApplyExceptionally {

        @Test
        @DisplayName("applyExceptionally with no failure")
        void applyExceptionallyNoFailure() {
            AtomicReference<Object> value = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            CompletableFuture<Integer> future = CompletableFuture.completedFuture(58);
            AsyncHelpers
                    .applyExceptionally(future, err -> new RuntimeException("!!! " + err.getMessage()))
                    .whenComplete((n, err) -> {
                        value.set(n);
                        error.set(err);
                    });

            assertEquals(58, value.get());
            assertNull(error.get());
        }

        @Test
        @DisplayName("applyExceptionally with a failure")
        void applyExceptionallyWithFailure() {
            AtomicReference<Object> value = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            CompletableFuture<?> future = AsyncHelpers.failedFuture(new IOException("boom")).toCompletableFuture();
            AsyncHelpers
                    .applyExceptionally(future, err -> new RuntimeException("!!! " + err.getMessage()))
                    .whenComplete((n, err) -> {
                        value.set(n);
                        error.set(err);
                    });

            assertNull(value.get());
            assertNotNull(error.get());
            assertTrue(error.get() instanceof RuntimeException);
            assertEquals("!!! boom", error.get().getMessage());
        }

        @Test
        @DisplayName("applyExceptionally with a failure and throw an exception")
        void applyExceptionallyAndThrow() {
            AtomicReference<Object> value = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            CompletableFuture<?> future = AsyncHelpers.failedFuture(new IOException("boom")).toCompletableFuture();
            AsyncHelpers
                    .applyExceptionally(future, err -> {
                        throw new RuntimeException("!!! " + err.getMessage());
                    })
                    .whenComplete((n, err) -> {
                        value.set(n);
                        error.set(err);
                    });

            assertNull(value.get());
            assertNotNull(error.get());
            assertTrue(error.get() instanceof RuntimeException);
            assertEquals("!!! boom", error.get().getMessage());
        }

        @Test
        @DisplayName("applyExceptionally with a null mapper")
        void applyExceptionallyWithNullMapper() {
            CompletableFuture<?> future = AsyncHelpers.failedFuture(new IOException("boom")).toCompletableFuture();
            assertThrows(NullPointerException.class, () -> AsyncHelpers.applyExceptionally(future, null));
        }
    }

    @Nested
    @DisplayName("Compose on exceptions")
    class ComposeExceptionally {

        @Test
        @DisplayName("composeExceptionally with no failure")
        void composeExceptionallyNoFailure() {
            AtomicReference<Object> value = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            CompletableFuture<Integer> future = CompletableFuture.completedFuture(58);
            AsyncHelpers
                    .composeExceptionally(future,
                            err -> AsyncHelpers.failedFuture(new RuntimeException("!!! " + err.getMessage())))
                    .whenComplete((n, err) -> {
                        value.set(n);
                        error.set(err);
                    });

            assertEquals(58, value.get());
            assertNull(error.get());
        }

        @Test
        @DisplayName("composeExceptionally with a failure")
        void composeExceptionallyWithFailure() {
            AtomicReference<Object> value = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            CompletionStage<Object> future = AsyncHelpers.failedFuture(new IOException("boom"));
            AsyncHelpers
                    .composeExceptionally(future,
                            err -> AsyncHelpers.failedFuture(new RuntimeException("!!! " + err.getMessage())))
                    .whenComplete((n, err) -> {
                        value.set(n);
                        error.set(err);
                    });

            assertNull(value.get());
            assertNotNull(error.get());
            assertTrue(error.get() instanceof RuntimeException);
            assertEquals("!!! boom", error.get().getMessage());
        }

        @Test
        @DisplayName("composeExceptionally with a failure then recover")
        void composeExceptionallyWithFailureThenRecover() {
            AtomicReference<Object> value = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            CompletionStage<Object> future = AsyncHelpers.failedFuture(new IOException("boom"));
            AsyncHelpers
                    .composeExceptionally(future, err -> CompletableFuture.completedFuture("Ok"))
                    .whenComplete((n, err) -> {
                        value.set(n);
                        error.set(err);
                    });

            assertNull(error.get());
            assertEquals("Ok", value.get());
        }

        @Test
        @DisplayName("composeExceptionally with a failure and throw an exception")
        void composeExceptionallyAndThrow() {
            AtomicReference<Object> value = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            CompletableFuture<?> future = AsyncHelpers.failedFuture(new IOException("boom")).toCompletableFuture();
            AsyncHelpers
                    .composeExceptionally(future, err -> {
                        throw new RuntimeException("!!! " + err.getMessage());
                    })
                    .whenComplete((n, err) -> {
                        value.set(n);
                        error.set(err);
                    });

            assertNull(value.get());
            assertNotNull(error.get());
            assertTrue(error.get() instanceof RuntimeException);
            assertEquals("!!! boom", error.get().getMessage());
        }

        @Test
        @DisplayName("composeExceptionally with a null mapper")
        void composeExceptionallyWithNullMapper() {
            CompletionStage<Object> future = AsyncHelpers.failedFuture(new IOException("boom"));
            assertThrows(NullPointerException.class, () -> AsyncHelpers.composeExceptionally(future, null));
        }
    }
}