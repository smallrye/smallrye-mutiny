package io.smallrye.reactive.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.test.MultiAssertSubscriber;

public class MultiCreateFromCompletionStageTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatTheCompletionStageCannotBeNull() {
        Multi.createFrom().completionStage((CompletionStage<String>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatTheCompletionStageSupplierCannotBeNull() {
        Multi.createFrom().deferredCompletionStage((Supplier<CompletionStage<String>>) null);
    }

    @Test
    public void testWithAValue() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.completedFuture("hello")).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithAsyncValue() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.supplyAsync(() -> "hello")).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.await().assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithEmpty() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.<String> completedFuture(null)).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithAsyncCompletionWithNull() {
        AtomicBoolean called = new AtomicBoolean();
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.runAsync(() -> called.set(true))).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.await().assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        assertThat(called).isTrue();
    }

    @Test
    public void testWithAValueProducedInSupplier() {
        AtomicInteger count = new AtomicInteger();

        Multi<String> multi = Multi.createFrom()
                .deferredCompletionStage(() -> CompletableFuture.completedFuture("hello-" + count.incrementAndGet()));
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertReceived("hello-1");
        subscriber2.assertHasNotReceivedAnyItem().assertNotTerminated().request(20)
                .assertCompletedSuccessfully().assertReceived("hello-2");
    }

    @Test
    public void testWithEmptyProducedInSupplier() {
        Multi<String> multi = Multi.createFrom().deferredCompletionStage(() -> CompletableFuture.completedFuture(null));
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        subscriber2.assertHasNotReceivedAnyItem().assertCompletedSuccessfully();
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().deferredCompletionStage(() -> {
            throw new IllegalStateException("boom");
        });
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().deferredCompletionStage(() -> null);
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }

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
        MultiAssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertNotTerminated()
                .cancel()
                .assertNotTerminated();
        assertThat(cancelled).isTrue();
    }
}
