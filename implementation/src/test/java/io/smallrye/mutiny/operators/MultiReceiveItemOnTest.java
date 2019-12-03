package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiReceiveItemOnTest {

    private ExecutorService executor;

    @BeforeMethod
    public void init() {
        executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("test-" + count.incrementAndGet());
                return thread;
            }
        });
    }

    @AfterTest
    public void cleanup() {
        executor.shutdownNow();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatExecutorCannotBeNull() {
        Multi.createFrom().item(1).emitOn(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatSubscribeOnExecutorCannotBeNull() {
        Multi.createFrom().item(1).subscribeOn(null);
    }

    // TODO Rejected execution does not forward a failure, it's caught by RX

    @Test
    public void testThatItemsAreDispatchedOnTheRightThread() {
        Set<String> itemThread = ConcurrentHashMap.newKeySet();
        Set<String> completionThread = ConcurrentHashMap.newKeySet();
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4)
                .emitOn(executor)
                .onItem().invoke(i -> itemThread.add(Thread.currentThread().getName()))
                .on().completion(() -> completionThread.add(Thread.currentThread().getName()))
                .subscribe().with(MultiAssertSubscriber.create(4))
                .await()
                .assertCompletedSuccessfully();

        await().until(() -> subscriber.items().size() == 4);
        assertThat(itemThread).allSatisfy(s -> assertThat(s).startsWith("test-"));
        assertThat(completionThread).allSatisfy(s -> assertThat(s).startsWith("test-"));
    }

    @Test
    public void testThatFailureAreDispatchedOnExecutor() {
        Set<String> itemThread = new LinkedHashSet<>();
        Set<String> failureThread = new LinkedHashSet<>();
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .emitOn(executor)
                .onItem().invoke(i -> itemThread.add(Thread.currentThread().getName()))
                .onFailure().invoke(f -> failureThread.add(Thread.currentThread().getName()))
                .subscribe().with(MultiAssertSubscriber.create(4))
                .await()
                .assertHasFailedWith(IOException.class, "boom");

        assertThat(itemThread).isEmpty();
        assertThat(failureThread).hasSize(1).allSatisfy(s -> assertThat(s).startsWith("test-"));
    }

    @Test
    public void testWithImmediate() {
        Multi.createFrom().items(1, 2, 3, 4)
                .emitOn(Runnable::run)
                .subscribe().with(MultiAssertSubscriber.create(4))
                .await()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testWithLargeNumberOfItems() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().range(0, 100_000)
                .emitOn(executor)
                .subscribe().with(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertCompletedSuccessfully();

        assertThat(subscriber.items()).hasSize(100_000);
        int current = -1;
        for (Integer i : subscriber.items()) {
            assertThat(i).isGreaterThan(current);
            current = i;
        }
    }

    @Test
    public void testSubscribeOn() {
        Multi.createFrom().items(1, 2, 3, 4)
                .subscribeOn(executor)
                .subscribe().with(MultiAssertSubscriber.create(4))
                .await()
                .assertReceived(1, 2, 3, 4);
    }

}
