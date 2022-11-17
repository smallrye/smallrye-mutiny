package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniOnItemTransformToUniWithEmitterTest {

    @Test
    public void testTransformToUniWithImmediateValue() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).onItem().<Integer> transformToUni(
                (v, e) -> e.complete(2))
                .subscribe().withSubscriber(test);
        test.assertCompleted().assertItem(2);
    }

    @Test
    public void testWithImmediateCancellation() {
        UniAssertSubscriber<Integer> test = new UniAssertSubscriber<>(true);
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().<Integer> transformToUni((v, e) -> {
            called.set(true);
            e.complete(2);
        }).subscribe().withSubscriber(test);
        test.assertNotTerminated();
        assertThat(called).isFalse();
    }

    @Test
    public void testWithAsyncEmitter() {
        UniAssertSubscriber<Integer> test1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> test2 = UniAssertSubscriber.create();
        AtomicInteger count = new AtomicInteger(2);
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni((v, e) -> new Thread(() -> e.complete(count.incrementAndGet())).start());
        uni.subscribe().withSubscriber(test1);
        uni.subscribe().withSubscriber(test2);
        assertThat(test1.awaitItem().getItem()).isBetween(3, 4);
        assertThat(test2.awaitItem().getItem()).isBetween(3, 4);
    }

    @Test
    public void testWithAsyncEmitterAndFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni((v, e) -> new Thread(() -> e.fail(new IOException("boom"))).start());
        uni.subscribe().withSubscriber(test);
        test.awaitFailure().assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testThatMapperIsNotCalledOnUpstreamFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().failure(new Exception("boom")).onItem().<Integer> transformToUni((v, e) -> {
            called.set(true);
            e.complete(2);
        }).subscribe().withSubscriber(test);
        test.awaitFailure().assertFailedWith(Exception.class, "boom");
        assertThat(called).isFalse();
    }

    @Test
    public void testWithAMapperThrowingAnException() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().<Integer> transformToUni((v, e) -> {
            called.set(true);
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(test);
        test.awaitFailure().assertFailedWith(IllegalStateException.class, "boom");
        assertThat(called).isTrue();
    }

    @Test
    public void testWithAMapperThrowingAnExceptionAfterEmittingAValue() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().<Integer> transformToUni((v, e) -> {
            called.set(true);
            e.complete(2);
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(test);
        assertThat(test.awaitItem().getItem()).isEqualTo(2);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatTheMapperCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).onItem()
                .transformToUni((BiConsumer<Integer, UniEmitter<? super Integer>>) null));
    }

    @Test
    public void testWithCancellationBeforeEmission() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni((v, e) -> future.whenComplete((x, f) -> e.complete(x)));
        uni.subscribe().withSubscriber(test);
        test.cancel();
        test.assertNotTerminated();
    }
}
