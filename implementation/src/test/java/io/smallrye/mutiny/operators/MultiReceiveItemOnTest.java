package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiReceiveItemOnTest {

    private ExecutorService executor;

    @BeforeEach
    public void init() {
        executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            final AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("test-" + count.incrementAndGet());
                return thread;
            }
        });
    }

    @AfterEach
    public void cleanup() {
        executor.shutdownNow();
    }

    @Test
    public void testThatExecutorCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1).emitOn(null));
    }

    @Test
    public void testThatRunSubscriptionOnExecutorCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1).runSubscriptionOn(null));
    }

    @Test
    public void testThatItemsAreDispatchedOnTheRightThread() {
        Set<String> itemThread = ConcurrentHashMap.newKeySet();
        Set<String> completionThread = ConcurrentHashMap.newKeySet();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4)
                .emitOn(executor)
                .onItem().invoke(i -> itemThread.add(Thread.currentThread().getName()))
                .onCompletion().invoke(() -> completionThread.add(Thread.currentThread().getName()))
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitCompletion();

        await().until(() -> subscriber.getItems().size() == 4);
        assertThat(itemThread).allSatisfy(s -> assertThat(s).startsWith("test-"));
        assertThat(completionThread).hasSizeGreaterThanOrEqualTo(1).allSatisfy(s -> assertThat(s).startsWith("test-"));
    }

    @Test
    public void testThatFailureAreDispatchedOnExecutor() {
        Set<String> itemThread = new LinkedHashSet<>();
        Set<String> failureThread = new LinkedHashSet<>();
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .emitOn(executor)
                .onItem().invoke(i -> itemThread.add(Thread.currentThread().getName()))
                .onFailure().invoke(f -> failureThread.add(Thread.currentThread().getName()))
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitFailure()
                .assertFailedWith(IOException.class, "boom");

        assertThat(itemThread).isEmpty();
        assertThat(failureThread).hasSizeGreaterThanOrEqualTo(1).allSatisfy(s -> assertThat(s).startsWith("test-"));
    }

    @Test
    public void testWithImmediate() {
        Multi.createFrom().items(1, 2, 3, 4)
                .emitOn(Runnable::run)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitCompletion()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testWithLargeNumberOfItems() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(0, 100_000)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion();

        assertThat(subscriber.getItems()).hasSize(100_000);
        int current = -1;
        for (Integer i : subscriber.getItems()) {
            assertThat(i).isGreaterThan(current);
            current = i;
        }
    }

    @Test
    public void testRunSubscriptionOn() {
        Multi.createFrom().items(1, 2, 3, 4)
                .runSubscriptionOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(4))
                .awaitCompletion()
                .assertItems(1, 2, 3, 4);
    }

}
