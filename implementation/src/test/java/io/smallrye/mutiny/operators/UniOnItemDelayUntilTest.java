package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class UniOnItemDelayUntilTest {

    private ScheduledExecutorService executor;

    private Uni<Void> delayed;

    @BeforeEach
    public void init() {
        executor = Executors.newScheduledThreadPool(4);
        delayed = Uni.createFrom().voidItem().onItem().delayIt()
                .onExecutor(executor)
                .until(x -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofMillis(10)));
    }

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void testWithNullFunction() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onItem().delayIt().until(null));
    }

    @Test
    public void testWithFunctionReturningNull() {
        assertThrows(NullPointerException.class,
                () -> Uni.createFrom().item(1).onItem().delayIt().until(x -> null).await().indefinitely());
    }

    @Test
    public void testWithFunctionThrowException() {
        assertThrows(IllegalStateException.class,
                () -> Uni.createFrom().item(1).onItem().delayIt().until(x -> {
                    throw new IllegalStateException("boom");
                }).await().indefinitely());
    }

    @Test
    public void testWithUpstreamFailure() {
        AtomicBoolean called = new AtomicBoolean();
        assertThatThrownBy(() -> Uni.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onItem().delayIt().until(i -> {
                    called.set(true);
                    return Uni.createFrom().nullItem();
                }).await().indefinitely()).hasMessageContaining("boom").isInstanceOf(IllegalStateException.class);
        assertThat(called).isFalse();
    }

    @Test
    public void testNoDelay() {
        int i = Uni.createFrom().item(1).onItem().delayIt().until(x -> Uni.createFrom().nullItem())
                .await().indefinitely();
        assertThat(i).isEqualTo(1);
    }

    @Test
    public void testWithDelay() {
        int i = Uni.createFrom().item(1).onItem().delayIt().until(x -> delayed)
                .await().indefinitely();
        assertThat(i).isEqualTo(1);
    }

    @Test
    public void testWithEmission() {
        AtomicReference<UniEmitter<?>> reference = new AtomicReference<>();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                .onItem().delayIt().until(i -> Uni.createFrom().emitter(reference::set))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        await().until(() -> reference.get() != null);
        subscriber.assertNotTerminated();
        reference.get().complete(null);
        subscriber.awaitItem().assertItem(1);
    }

    @Test
    public void testWithDelayAndExecutor() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "my-thread"));
        AtomicReference<String> thread = new AtomicReference<>();
        int i = Uni.createFrom().item(1)
                .emitOn(executor)
                .onItem().delayIt().onExecutor(exec).until(x -> Uni.createFrom().nullItem()
                        .onItem().invoke(ignored -> thread.set(Thread.currentThread().getName())))
                .await().indefinitely();
        assertThat(i).isEqualTo(1);
        assertThat(thread.get()).isEqualTo("my-thread");
        exec.shutdownNow();
    }

    @Test
    public void testCancellationDuringWaitingTime() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                // It will never emit the item
                .onItem().delayIt().until(i -> Uni.createFrom().nothing())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertNotTerminated();
        subscriber.cancel();
    }

    @Test
    public void testImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                // It will never emit the item
                .onItem().delayIt().until(i -> {
                    called.set(true);
                    return Uni.createFrom().nothing();
                })
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotTerminated();
        assertThat(called).isFalse();
    }

    @Test
    public void testCancellationDuringWaitingTimeWithEmissionAfterward() {
        AtomicReference<UniEmitter<?>> reference = new AtomicReference<>();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                .onItem().delayIt().until(i -> Uni.createFrom().emitter(reference::set))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        await().until(() -> reference.get() != null);
        subscriber.assertNotTerminated();
        subscriber.cancel();
        reference.get().complete(null);
        subscriber.assertNotTerminated();
    }
}
