package io.smallrye.mutiny.nativetests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledInNativeImage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class SmokeTests {

    @Test
    public void concatMap() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        Multi.createFrom().range(1, 10_000)
                .onItem().transformToMultiAndConcatenate(n -> Multi.createFrom().range(n + 2, n + 4))
                .subscribe().withSubscriber(subscriber);

        subscriber.request(5);
        subscriber.assertItems(3, 4, 4, 5, 5);

        subscriber.request(Long.MAX_VALUE);
        subscriber.assertCompleted();
        assertEquals(19998, subscriber.getItems().size());
    }

    @Test
    @EnabledInNativeImage
    public void emitterFailingInNative() {
        assertThrows(ExceptionInInitializerError.class, this::emitterScenario);
    }

    @Test
    public void emitterWorkingInNative() {
        runWithAtomicQueues(this::emitterScenario);
    }

    private void runWithAtomicQueues(Runnable action) {
        Infrastructure.setUseUnsafeForQueues(false);
        try {
            action.run();
        } finally {
            Infrastructure.setUseUnsafeForQueues(true);
        }
    }

    private void emitterScenario() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        Multi.createFrom().<Integer> emitter(emitter -> {
            new Thread(() -> {
                Random random = new Random();
                for (int i = 0; i < 10_000; i++) {
                    emitter.emit(random.nextInt());
                }
                emitter.complete();
            }).start();
        }).subscribe().withSubscriber(subscriber);

        subscriber.request(Long.MAX_VALUE);
        subscriber.awaitCompletion();
        assertEquals(10_000, subscriber.getItems().size());
    }

    @Test
    @EnabledInNativeImage
    public void overflowFailingInNative() {
        assertThrows(NoClassDefFoundError.class, this::overflowScenario);
    }

    @Test
    public void overflowWorkingInNative() {
        runWithAtomicQueues(this::overflowScenario);
    }

    private void overflowScenario() {
        AssertSubscriber<Long> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .onOverflow().bufferUnconditionally()
                .subscribe().withSubscriber(AssertSubscriber.create(5));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        subscriber.cancel();
    }
}
