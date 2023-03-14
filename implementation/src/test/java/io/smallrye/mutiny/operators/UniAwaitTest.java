package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import junit5.support.InfrastructureResource;

public class UniAwaitTest {

    @Test
    @Timeout(10)
    public void testAwaitingOnAnAlreadyResolvedUni() {
        assertThat(Uni.createFrom().item(1).await().indefinitely()).isEqualTo(1);
    }

    @Test
    @Timeout(10)
    public void testAwaitingOnAnAlreadyResolvedWitNullUni() {
        assertThat(Uni.createFrom().item((Object) null).await().indefinitely()).isNull();
    }

    @Test
    @Timeout(10)
    public void testAwaitingOnAnAlreadyFailedUni() {
        try {
            Uni.createFrom().failure(new IOException("boom")).await().indefinitely();
            fail("Exception expected");
        } catch (CompletionException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageEndingWith("boom");
        }
    }

    @Test
    @Timeout(10)
    public void testAwaitingOnAnAsyncUni() {
        assertThat(
                Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(1)).start()).await()
                        .indefinitely())
                .isEqualTo(1);
    }

    @Test
    @Timeout(10)
    public void testAwaitingOnAnAsyncFailingUni() {
        try {
            Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.fail(new IOException("boom"))).start()).await()
                    .indefinitely();
            fail("Exception expected");
        } catch (CompletionException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageEndingWith("boom");
        }
    }

    @Test
    @Timeout(10)
    public void testAwaitWithTimeOut() {
        assertThat(Uni.createFrom().item(1).await().atMost(Duration.ofMillis(1000))).isEqualTo(1);
    }

    @Test
    @Timeout(10)
    public void testTimeout() {
        assertThrows(TimeoutException.class, () -> Uni.createFrom().nothing().await().atMost(Duration.ofMillis(10)));
    }

    @Test
    @Timeout(5)
    public void testInterruptedTimeout() {
        AtomicBoolean awaiting = new AtomicBoolean();
        AtomicReference<RuntimeException> exception = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                awaiting.set(true);
                Uni.createFrom().nothing().await().atMost(Duration.ofMillis(1000));
            } catch (RuntimeException e) {
                exception.set(e);
            }
        });
        thread.start();
        await().untilTrue(awaiting);
        thread.interrupt();
        await().until(() -> exception.get() != null);
        assertThat(exception.get()).hasCauseInstanceOf(InterruptedException.class);
    }

    @Test
    public void testAwaitAsOptionalWithResult() {
        assertThat(
                Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(1)).start()).await().asOptional()
                        .indefinitely())
                .contains(1);
    }

    @Test
    public void testAwaitAsOptionalWithFailure() {
        try {
            Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.fail(new IOException("boom"))).start())
                    .await().asOptional().indefinitely();
            fail("Exception expected");
        } catch (CompletionException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageEndingWith("boom");
        }
    }

    @Test
    public void testAwaitAsOptionalWithNull() {
        assertThat(
                Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(null)).start()).await()
                        .asOptional().indefinitely())
                .isEmpty();
    }

    @Test
    public void testAwaitAsOptionalWithTimeout() {
        assertThat(
                Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(1)).start()).await().asOptional()
                        .atMost(Duration.ofMillis(1000)))
                .contains(1);
    }

    @Test
    @Timeout(10)
    public void testTimeoutAndOptional() {
        assertThrows(TimeoutException.class,
                () -> Uni.createFrom().nothing().await().asOptional().atMost(Duration.ofMillis(10)));
    }

    @Test
    public void testInvalidDurations() {
        Uni<Integer> one = Uni.createFrom().item(1);

        // Null duration means infinite
        assertThat(one.await().atMost(null)).isEqualTo(1);

        assertThatThrownBy(() -> one.await().atMost(Duration.ofMillis(-2000)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duration");

        assertThatThrownBy(() -> one.await().atMost(Duration.ofSeconds(0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duration");
    }

    @Nested
    @ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
    class ThreadBlockingTest {

        @BeforeEach
        void reset() {
            Infrastructure.resetCanCallerThreadBeBlockedSupplier();
        }

        @Test
        void checkAllowByDefault() throws InterruptedException {
            Uni<Integer> one = Uni.createFrom().item(1);
            AtomicReference<Throwable> exception = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try {
                    assertThat(one.await().indefinitely()).isEqualTo(1);
                } catch (Throwable err) {
                    exception.set(err);
                }
            }, "my-thread");
            thread.start();
            thread.join();
            assertThat(exception.get()).isNull();
        }

        @Test
        void checkAllow() throws InterruptedException {
            Infrastructure.setCanCallerThreadBeBlockedSupplier(() -> !Thread.currentThread().getName().contains("-forbidden-"));
            Uni<Integer> one = Uni.createFrom().item(1);
            AtomicReference<Throwable> exception = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try {
                    assertThat(one.await().indefinitely()).isEqualTo(1);
                } catch (Throwable err) {
                    exception.set(err);
                }
            }, "my-thread");
            thread.start();
            thread.join();
            assertThat(exception.get()).isNull();
        }

        @Test
        void checkForbid() throws InterruptedException {
            Infrastructure.setCanCallerThreadBeBlockedSupplier(() -> !Thread.currentThread().getName().contains("-forbidden-"));
            Uni<Integer> one = Uni.createFrom().item(1);
            AtomicReference<Throwable> exception = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try {
                    assertThat(one.await().indefinitely()).isEqualTo(1);
                } catch (Throwable err) {
                    exception.set(err);
                }
            }, "my-forbidden-thread");
            thread.start();
            thread.join();
            assertThat(exception.get())
                    .isNotNull()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("The current thread cannot be blocked: my-forbidden-thread");
        }
    }
}
