package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiCreateFromCompletionStageTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheCompletionStageCannotBeNull() {
        Multi.createFrom().completionStage((CompletionStage<String>) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheCompletionStageSupplierCannotBeNull() {
        Multi.createFrom().completionStage((Supplier<CompletionStage<String>>) null);
    }

    @Test
    public void testWithAValue() {
        AssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.completedFuture("hello")).subscribe()
                .withSubscriber(AssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithAsyncValue() {
        AssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.supplyAsync(() -> "hello")).subscribe()
                .withSubscriber(AssertSubscriber.create(1));
        subscriber.await().assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithEmpty() {
        AssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.<String> completedFuture(null)).subscribe()
                .withSubscriber(AssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithAsyncCompletionWithNull() {
        AtomicBoolean called = new AtomicBoolean();
        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.runAsync(() -> called.set(true))).subscribe()
                .withSubscriber(AssertSubscriber.create(1));
        subscriber.await().assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        assertThat(called).isTrue();
    }

    @Test
    public void testWithAValueProducedInSupplier() {
        AtomicInteger count = new AtomicInteger();

        Multi<String> multi = Multi.createFrom()
                .completionStage(() -> CompletableFuture.completedFuture("hello-" + count.incrementAndGet()));
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(AssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertReceived("hello-1");
        subscriber2.assertHasNotReceivedAnyItem().assertNotTerminated().request(20)
                .assertCompletedSuccessfully().assertReceived("hello-2");
    }

    @Test
    public void testWithEmptyProducedInSupplier() {
        Multi<String> multi = Multi.createFrom().completionStage(() -> CompletableFuture.completedFuture(null));
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create(1));
        AssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(AssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        subscriber2.assertHasNotReceivedAnyItem().assertCompletedSuccessfully();
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().completionStage(() -> {
            throw new IllegalStateException("boom");
        });
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().completionStage(() -> null);
        AssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testCancellation() {
        AtomicBoolean cancelled = new AtomicBoolean();
        CompletableFuture<Integer> never = new CompletableFuture<Integer>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                cancelled.set(true);
                return super.cancel(mayInterruptIfRunning);
            }
        };

        Multi<Integer> multi = Multi.createFrom().completionStage(never);
        AssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(1));

        subscriber.assertNotTerminated()
                .cancel()
                .assertNotTerminated();
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testWithException() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Multi.createFrom().completionStage(cs).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().completeExceptionally(new IOException("boom"));
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithRuntimeException() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Multi.createFrom().completionStage(cs).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().completeExceptionally(new IllegalArgumentException("boom"));
        subscriber.assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWithExceptionThrownByAStage() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Multi.createFrom().completionStage(() -> cs
                .thenApply(String::toUpperCase)
                .<String> thenApply(s -> {
                    throw new IllegalStateException("boom");
                })).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().complete("bonjour");
        subscriber.assertHasFailedWith(IllegalStateException.class, "boom");
    }
}
