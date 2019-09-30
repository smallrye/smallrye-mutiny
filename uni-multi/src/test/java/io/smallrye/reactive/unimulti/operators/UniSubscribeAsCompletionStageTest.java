package io.smallrye.reactive.unimulti.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import io.smallrye.reactive.unimulti.Uni;

public class UniSubscribeAsCompletionStageTest {

    private ScheduledExecutorService executor;

    @After
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    public void testWithImmediateValue() {
        CompletableFuture<Integer> future = Uni.createFrom().item(1).subscribe().asCompletionStage();
        assertThat(future).isNotNull();
        assertThat(future).isCompletedWithValue(1);
    }

    @Test
    public void testShortcut() {
        CompletableFuture<Integer> future = Uni.createFrom().item(1).subscribeAsCompletionStage();
        assertThat(future).isNotNull();
        assertThat(future).isCompletedWithValue(1);
    }

    @Test
    public void testWithImmediateNullValue() {
        CompletableFuture<Void> future = Uni.createFrom().item((Void) null).subscribe().asCompletionStage();
        assertThat(future).isNotNull();
        assertThat(future).isCompletedWithValue(null);
    }

    @Test
    public void testWithImmediateFailure() {
        CompletableFuture<Integer> future = Uni.createFrom().<Integer> failure(new IOException("boom")).subscribe()
                .asCompletionStage();
        assertThat(future).isNotNull();
        try {
            future.join();
            fail("exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        }

    }

    @Test
    public void testThatSubscriptionsAreNotShared() {
        AtomicInteger count = new AtomicInteger(1);
        Uni<Integer> deferred = Uni.createFrom().deferred(() -> Uni.createFrom().item(count.getAndIncrement()));
        CompletionStage<Integer> cs1 = deferred.subscribe().asCompletionStage();
        CompletionStage<Integer> cs2 = deferred.subscribe().asCompletionStage();
        assertThat(cs1).isNotNull();
        assertThat(cs2).isNotNull();

        assertThat(cs1).isCompletedWithValue(1);
        assertThat(cs2).isCompletedWithValue(2);
    }

    @Test
    public void testThatTwoSubscribersWithCache() {
        AtomicInteger count = new AtomicInteger(1);
        Uni<Integer> cached = Uni.createFrom().deferred(() -> Uni.createFrom().item(count.getAndIncrement())).cache();
        CompletionStage<Integer> cs1 = cached.subscribe().asCompletionStage();
        CompletionStage<Integer> cs2 = cached.subscribe().asCompletionStage();
        assertThat(cs1).isNotNull();
        assertThat(cs2).isNotNull();
        assertThat(cs1).isCompletedWithValue(1);
        assertThat(cs1).isCompletedWithValue(1);
    }

    @Test
    public void testCancellationWithImmediateValue() {
        AtomicInteger value = new AtomicInteger(-1);
        CompletableFuture<Integer> future = Uni.createFrom().item(1).subscribe().asCompletionStage()
                .whenComplete((res, fail) -> value.set(res));
        future.cancel(false);
        assertThat(future).isNotCancelled(); // Too late.
        assertThat(value).hasValue(1);
    }

    @Test
    public void testCancellationWithAsyncValue() {
        executor = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger value = new AtomicInteger(-1);
        CompletableFuture<Integer> future = Uni.createFrom().item(1)
                .onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(100))
                .emitOn(executor)
                .subscribe().asCompletionStage()
                .whenComplete((res, fail) -> value.set(res));

        future.cancel(false);
        assertThat(value).hasValue(-1);
    }

    @Test
    public void testWithAsyncValue() {
        executor = Executors.newSingleThreadScheduledExecutor();
        CompletableFuture<Integer> future = Uni.createFrom().item(1)
                .emitOn(executor).subscribe().asCompletionStage();
        await().until(future::isDone);
        assertThat(future).isCompletedWithValue(1);
    }

    @Test
    public void testWithAsyncNullValue() {
        executor = Executors.newSingleThreadScheduledExecutor();
        CompletableFuture<Void> future = Uni.createFrom().item((Void) null).emitOn(executor)
                .subscribe().asCompletionStage();
        await().until(future::isDone);
        assertThat(future).isCompletedWithValue(null);
    }

    @Test
    public void testWithAsyncFailure() {
        executor = Executors.newSingleThreadScheduledExecutor();
        CompletableFuture<Integer> future = Uni.createFrom().<Integer> failure(new IOException("boom"))
                .emitOn(executor).subscribe().asCompletionStage();
        await().until(future::isDone);
        assertThat(future).isCompletedExceptionally();
    }

}
