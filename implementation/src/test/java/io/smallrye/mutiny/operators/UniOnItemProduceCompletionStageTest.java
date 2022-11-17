package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class UniOnItemProduceCompletionStageTest {

    @Test
    public void testProduceCompletionStageWithImmediateValue() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1)
                .onItem().transformToUni(n -> Uni.createFrom().completionStage(CompletableFuture.completedFuture(2)))
                .subscribe().withSubscriber(test);
        test.assertCompleted().assertItem(2);
    }

    @Test
    public void testWithImmediateCancellation() {
        UniAssertSubscriber<Integer> test = new UniAssertSubscriber<>(true);
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().transformToUni(v -> {
            called.set(true);
            return Uni.createFrom().completionStage(CompletableFuture.completedFuture(2));
        }).subscribe().withSubscriber(test);
        test.assertNotTerminated();
        assertThat(called).isFalse();
    }

    @Test
    public void testWithACompletionStageResolvedAsynchronously() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni(v -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 42)));
        uni.subscribe().withSubscriber(test);
        assertThat(test.awaitItem().getItem()).isEqualTo(42);
    }

    @Test
    public void testWithACompletionStageResolvedAsynchronouslyWithAFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem().transformToUni(
                v -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                    throw new IllegalStateException("boom");
                })));
        uni.subscribe().withSubscriber(test);
        test.awaitFailure().assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testThatMapperIsNotCalledOnUpstreamFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().failure(new Exception("boom")).onItem().transformToUni(v -> {
            called.set(true);
            return Uni.createFrom().completionStage(CompletableFuture.completedFuture(2));
        }).subscribe().withSubscriber(test);
        test.awaitFailure().assertFailedWith(Exception.class, "boom");
        assertThat(called).isFalse();
    }

    @Test
    public void testWithAMapperThrowingAnException() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1)
                .onItem().<Integer> transformToUni(v -> {
                    called.set(true);
                    throw new IllegalStateException("boom");
                })
                .subscribe().withSubscriber(test);
        test.awaitFailure().assertFailedWith(IllegalStateException.class, "boom");
        assertThat(called).isTrue();
    }

    @Test
    public void testWithAMapperReturningNull() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1)
                .onItem().<Integer> transformToUni(v -> {
                    called.set(true);
                    return null;
                }).subscribe().withSubscriber(test);
        test.awaitFailure().assertFailedWith(NullPointerException.class, "");
        assertThat(called).isTrue();
    }

    @Test
    public void testThatTheMapperCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onItem().transformToUni((Function<? super Integer, Uni<?>>) null));
    }

    @Test
    public void testWithCancellationBeforeEmission() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean cancelled = new AtomicBoolean();
        CompletableFuture<Integer> future = new CompletableFuture<Integer>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                cancelled.set(true);
                return true;
            }
        };

        Uni<Integer> uni = Uni.createFrom().item(1).onItem().transformToUni(v -> Uni.createFrom().completionStage(future));
        uni.subscribe().withSubscriber(test);
        test.cancel();
        test.assertNotTerminated();
        assertThat(cancelled).isTrue();
    }
}
