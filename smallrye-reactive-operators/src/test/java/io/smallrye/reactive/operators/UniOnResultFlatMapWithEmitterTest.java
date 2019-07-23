package io.smallrye.reactive.operators;


import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniEmitter;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

public class UniOnResultFlatMapWithEmitterTest {

    @Test
    public void testFlatMapWithImmediateValue() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        Uni.createFrom().result(1).onResult().<Integer>mapToUni(
                (v, e) -> e.result(2)
        )
                .subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertResult(2).assertNoFailure();
    }

    @Test
    public void testWithImmediateCancellation() {
        AssertSubscriber<Integer> test = new AssertSubscriber<>(true);
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().result(1).onResult().<Integer>mapToUni((v, e) -> {
            called.set(true);
            e.result(2);
        }).subscribe().withSubscriber(test);
        test.assertNotCompleted();
        assertThat(called).isFalse();
    }

    @Test
    public void testWithAsyncEmitter() {
        AssertSubscriber<Integer> test1 = AssertSubscriber.create();
        AssertSubscriber<Integer> test2 = AssertSubscriber.create();
        AtomicInteger count = new AtomicInteger(2);
        Uni<Integer> uni = Uni.createFrom().result(1).onResult().mapToUni((v, e) ->
                new Thread(() -> e.result(count.incrementAndGet())).start()
        );
        uni.subscribe().withSubscriber(test1);
        uni.subscribe().withSubscriber(test2);
        test1.await().assertCompletedSuccessfully().assertResult(3).assertNoFailure();
        test2.await().assertCompletedSuccessfully().assertResult(4).assertNoFailure();
    }

    @Test
    public void testWithAsyncEmitterAndFailure() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().result(1).onResult()
                .mapToUni((v, e) -> new Thread(() -> e.failure(new IOException("boom"))).start());
        uni.subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testThatMapperIsNotCalledOnUpstreamFailure() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().failure(new Exception("boom")).onResult().<Integer>mapToUni((v, e) -> {
            called.set(true);
            e.result(2);
        }).subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(Exception.class, "boom");
        assertThat(called).isFalse();
    }

    @Test
    public void testWithAMapperThrowingAnException() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().result(1).onResult().<Integer>mapToUni((v, e) -> {
            called.set(true);
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(called).isTrue();
    }

    @Test
    public void testWithAMapperThrowingAnExceptionAfterEmittingAValue() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().result(1).onResult().<Integer>mapToUni((v, e) -> {
            called.set(true);
            e.result(2);
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(test);
        test.await().assertCompletedSuccessfully().assertResult(2).assertNoFailure();
        assertThat(called).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatTheMapperCannotBeNull() {
        Uni.createFrom().result(1).onResult().mapToUni((BiConsumer<Integer, UniEmitter<? super Integer>>) null);
    }

    @Test
    public void testWithCancellationBeforeEmission() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().result(1).onResult().mapToUni((v, e) -> future.whenComplete((x, f) -> e.result(x)));
        uni.subscribe().withSubscriber(test);
        test.cancel();
        test.assertNotCompleted();
    }
}