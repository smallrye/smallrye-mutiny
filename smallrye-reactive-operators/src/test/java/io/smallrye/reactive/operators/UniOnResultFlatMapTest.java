package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class UniOnResultFlatMapTest {

    @Test
    public void testFlatMapWithImmediateValue() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        Uni.createFrom().result(1).onResult().mapToUni(v -> Uni.createFrom().result(2)).subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertResult(2).assertNoFailure();
    }

    @Test
    public void testWithImmediateCancellation() {
        AssertSubscriber<Integer> test = new AssertSubscriber<>(true);
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().result(1).onResult().mapToUni(v -> {
            called.set(true);
            return Uni.createFrom().result(2);
        }).subscribe().withSubscriber(test);
        test.assertNotCompleted();
        assertThat(called).isFalse();
    }

    @Test
    public void testWithADeferredUi() {
        AssertSubscriber<Integer> test1 = AssertSubscriber.create();
        AssertSubscriber<Integer> test2 = AssertSubscriber.create();
        AtomicInteger count = new AtomicInteger(2);
        Uni<Integer> uni = Uni.createFrom().result(1).onResult().mapToUni(v -> Uni.createFrom().deferred(() -> Uni.createFrom().result(count.incrementAndGet())));
        uni.subscribe().withSubscriber(test1);
        uni.subscribe().withSubscriber(test2);
        test1.assertCompletedSuccessfully().assertResult(3).assertNoFailure();
        test2.assertCompletedSuccessfully().assertResult(4).assertNoFailure();
    }

    @Test
    public void testWithAnUniResolvedAsynchronously() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().result(1).onResult().mapToUni(v -> Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.result(42)).start()));
        uni.subscribe().withSubscriber(test);
        test.await().assertCompletedSuccessfully().assertResult(42).assertNoFailure();
    }

    @Test
    public void testWithAnUniResolvedAsynchronouslyWithAFailure() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().result(1).onResult().mapToUni(v -> Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.failure(new IOException("boom"))).start()));
        uni.subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testThatMapperIsNotCalledOnUpstreamFailure() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().failure(new Exception("boom")).onResult().mapToUni(v -> {
            called.set(true);
            return Uni.createFrom().result(2);
        }).subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(Exception.class, "boom");
        assertThat(called).isFalse();
    }

    @Test
    public void testWithAMapperThrowingAnException() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().result(1)
            .onResult().<Integer>mapToUni(v -> {
                called.set(true);
                throw new IllegalStateException("boom");
            })
            .subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(called).isTrue();
    }

    @Test
    public void testWithAMapperReturningNull() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().result(1)
                .onResult().<Integer>mapToUni(v -> {
                    called.set(true);
                    return null;
                }).subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(NullPointerException.class, "");
        assertThat(called).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatTheMapperCannotBeNull() {
        Uni.createFrom().result(1).onResult().mapToUni((Function<Integer, Uni<?>>) null);
    }

    @Test
    public void testWithCancellationBeforeEmission() {
        AssertSubscriber<Integer> test = AssertSubscriber.create();
        AtomicBoolean cancelled = new AtomicBoolean();
        @SuppressWarnings("unchecked")
        CompletableFuture<Integer> future = new CompletableFuture() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                cancelled.set(true);
                return true;
            }
        };

        Uni<Integer> uni = Uni.createFrom().result(1).onResult().mapToUni(v -> Uni.createFrom().completionStage(future));
        uni.subscribe().withSubscriber(test);
        test.cancel();
        test.assertNotCompleted();
        assertThat(cancelled).isTrue();
    }
}