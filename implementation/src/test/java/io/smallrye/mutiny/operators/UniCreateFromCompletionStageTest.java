package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.Test;

import io.smallrye.mutiny.Uni;

public class UniCreateFromCompletionStageTest {

    @Test
    public void testThatNullValueAreAccepted() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().complete(null);
        ts.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testWithNonNullValue() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().complete("1");
        ts.assertCompletedSuccessfully().assertItem("1");
    }

    @Test
    public void testWithException() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().completeExceptionally(new IOException("boom"));
        ts.assertFailure(IOException.class, "boom");
    }

    @Test
    public void testThatNullValueAreAcceptedWithSupplier() {
        UniAssertSubscriber<Void> ts = UniAssertSubscriber.create();
        Uni.createFrom().<Void> deferredCompletionStage(() -> CompletableFuture.completedFuture(null)).subscribe()
                .withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testWithNonNullValueWithSupplier() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().deferredCompletionStage(() -> cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().complete("1");
        ts.assertCompletedSuccessfully().assertItem("1");
    }

    @Test
    public void testWithExceptionWithSupplier() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().deferredCompletionStage(() -> cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().completeExceptionally(new IOException("boom"));
        ts.assertFailure(IOException.class, "boom");
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscription() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        cs.complete(1);
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().consume(i -> called.set(true));

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscriptionWithSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();

        Uni<Integer> uni = Uni.createFrom().deferredCompletionStage(() -> {
            called.set(true);
            return cs;
        })
                .onItem().consume(i -> called.set(true));

        assertThat(called).isFalse();

        cs.complete(1);

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatSubscriberIsIncompleteIfTheStageDoesNotEmit() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().consume(i -> called.set(true));

        assertThat(called).isFalse();
        uni.subscribe().withSubscriber(ts);
        assertThat(called).isFalse();
        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberIsIncompleteIfTheStageDoesNotEmitFromSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().deferredCompletionStage(() -> cs)
                .onItem().consume(i -> called.set(true));

        assertThat(called).isFalse();
        uni.subscribe().withSubscriber(ts);
        assertThat(called).isFalse();
        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberCanCancelBeforeEmission() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().consume(i -> {
                });

        uni.subscribe().withSubscriber(ts);
        ts.cancel();

        cs.complete(1);

        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberCanCancelBeforeEmissionWithSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().deferredCompletionStage(() -> cs);
        uni.subscribe().withSubscriber(ts);
        ts.cancel();

        cs.complete(1);
        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberCanCancelAfterEmission() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().consume(i -> called.set(true));

        uni.subscribe().withSubscriber(ts);
        cs.complete(1);
        ts.cancel();
        assertThat(called).isTrue();
        ts.assertItem(1);
    }

    @Test
    public void testThatSubscriberCanCancelAfterEmissionWithSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().deferredCompletionStage(() -> cs);

        uni.subscribe().withSubscriber(ts);
        cs.complete(1);
        ts.cancel();

        ts.assertItem(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatCompletionStageCannotBeNull() {
        Uni.createFrom().completionStage((CompletionStage<Void>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatCompletionStageSupplierCannotBeNull() {
        Uni.createFrom().deferredCompletionStage((Supplier<CompletionStage<Void>>) null);
    }

    @Test
    public void testThatCompletionStageSupplierCannotReturnNull() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().deferredCompletionStage(() -> null);

        uni.subscribe().withSubscriber(ts);
        ts.assertFailure(NullPointerException.class, "");
    }

}
