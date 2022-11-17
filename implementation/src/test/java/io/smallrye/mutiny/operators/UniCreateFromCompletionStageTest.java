package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class UniCreateFromCompletionStageTest {

    @Test
    public void testThatNullValueAreAccepted() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().complete(null);
        subscriber.assertCompleted().assertItem(null);
    }

    @Test
    public void testWithNonNullValue() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().complete("1");
        subscriber.assertCompleted().assertItem("1");
    }

    @Test
    public void testWithException() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().completeExceptionally(new IOException("boom"));
        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionThrownByAStage() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(() -> cs
                .thenApply(String::toUpperCase)
                .<String> thenApply(s -> {
                    throw new IllegalStateException("boom");
                })).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().complete("bonjour");
        subscriber.assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testThatNullValueAreAcceptedWithSupplier() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Void> completionStage(() -> CompletableFuture.completedFuture(null)).subscribe()
                .withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(null);
    }

    @Test
    public void testWithNonNullValueWithSupplier() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(() -> cs).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().complete("1");
        subscriber.assertCompleted().assertItem("1");
    }

    @Test
    public void testWithExceptionWithSupplier() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(() -> cs).subscribe().withSubscriber(subscriber);
        cs.toCompletableFuture().completeExceptionally(new IOException("boom"));
        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionInSupplier() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<String> completionStage(() -> {
            throw new NullPointerException("boom");
        }).subscribe().withSubscriber(subscriber);
        subscriber.assertFailedWith(NullPointerException.class, "boom");
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscription() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        cs.complete(1);
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().invoke(i -> called.set(true));

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscriptionWithSupplier() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();

        Uni<Integer> uni = Uni.createFrom().completionStage(() -> {
            called.set(true);
            return cs;
        })
                .onItem().invoke(i -> called.set(true));

        assertThat(called).isFalse();

        cs.complete(1);

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(subscriber);
        subscriber.assertCompleted().assertItem(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatSubscriberIsIncompleteIfTheStageDoesNotEmit() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().invoke(i -> called.set(true));

        assertThat(called).isFalse();
        uni.subscribe().withSubscriber(subscriber);
        assertThat(called).isFalse();
        subscriber.assertNotTerminated();
    }

    @Test
    public void testThatSubscriberIsIncompleteIfTheStageDoesNotEmitFromSupplier() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> cs)
                .onItem().invoke(i -> called.set(true));

        assertThat(called).isFalse();
        uni.subscribe().withSubscriber(subscriber);
        assertThat(called).isFalse();
        subscriber.assertNotTerminated();
    }

    @Test
    public void testThatSubscriberCanCancelBeforeEmission() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().invoke(i -> {
                });

        uni.subscribe().withSubscriber(subscriber);
        subscriber.cancel();

        cs.complete(1);

        subscriber.assertNotTerminated();
    }

    @Test
    public void testThatSubscriberCanCancelBeforeEmissionWithSupplier() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> cs);
        uni.subscribe().withSubscriber(subscriber);
        subscriber.cancel();

        cs.complete(1);
        subscriber.assertNotTerminated();
    }

    @Test
    public void testThatSubscriberCanCancelAfterEmission() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().invoke(i -> called.set(true));

        uni.subscribe().withSubscriber(subscriber);
        cs.complete(1);
        subscriber.cancel();
        assertThat(called).isTrue();
        subscriber.assertItem(1);
    }

    @Test
    public void testThatSubscriberCanCancelAfterEmissionWithSupplier() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> cs);

        uni.subscribe().withSubscriber(subscriber);
        cs.complete(1);
        subscriber.cancel();

        subscriber.assertItem(1);
    }

    @Test
    public void testThatCompletionStageCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().completionStage((CompletionStage<Void>) null));
    }

    @Test
    public void testThatCompletionStageSupplierCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().completionStage((Supplier<CompletionStage<Void>>) null));
    }

    @Test
    public void testThatCompletionStageSupplierCannotReturnNull() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> null);

        uni.subscribe().withSubscriber(subscriber);
        subscriber.assertFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWithSharedState() {
        UniAssertSubscriber<Integer> subscriber1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> subscriber2 = UniAssertSubscriber.create();
        AtomicInteger shared = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> shared,
                state -> CompletableFuture.completedFuture(state.incrementAndGet()));

        assertThat(shared).hasValue(0);
        uni.subscribe().withSubscriber(subscriber1);
        assertThat(shared).hasValue(1);
        subscriber1.assertCompleted().assertItem(1);
        uni.subscribe().withSubscriber(subscriber2);
        assertThat(shared).hasValue(2);
        subscriber2.assertCompleted().assertItem(2);
    }

    @Test
    public void testWithSharedStateProducingFailure() {
        UniAssertSubscriber<Integer> subscriber1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> subscriber2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> {
            throw new IllegalStateException("boom");
        };

        Uni<Integer> uni = Uni.createFrom().completionStage(boom,
                state -> CompletableFuture.completedFuture(state.incrementAndGet()));

        uni.subscribe().withSubscriber(subscriber1);
        subscriber1.assertFailedWith(IllegalStateException.class, "boom");
        uni.subscribe().withSubscriber(subscriber2);
        subscriber2.assertFailedWith(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testWithSharedStateProducingNull() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> null;

        Uni<Integer> uni = Uni.createFrom().completionStage(boom,
                state -> CompletableFuture.completedFuture(state.incrementAndGet()));

        uni.subscribe().withSubscriber(s1);
        s1.assertFailedWith(NullPointerException.class, "supplier");
        uni.subscribe().withSubscriber(s2);
        s2.assertFailedWith(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testThatStateSupplierCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().completionStage(null,
                x -> CompletableFuture.completedFuture("x")));
    }

    @Test
    public void testThatFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().completionStage(() -> "hello",
                null));
    }

}
