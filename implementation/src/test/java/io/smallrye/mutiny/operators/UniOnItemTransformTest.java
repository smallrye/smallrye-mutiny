package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniOnItemTransformTest {

    private final Uni<Integer> one = Uni.createFrom().item(1);

    @Test
    public void testThatMapperMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).map(null));
    }

    @Test
    public void testThatSourceMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new UniOnItemTransform<>(null, Function.identity()));
    }

    @Test
    public void testSimpleMapping() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        one.map(v -> v + 1).subscribe().withSubscriber(subscriber);

        subscriber.assertCompletedSuccessfully()
                .assertItem(2);
    }

    @Test
    public void testWithTwoSubscribers() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.map(v -> v + count.incrementAndGet());
        uni.subscribe().withSubscriber(s1);
        uni.subscribe().withSubscriber(s2);

        s1.assertCompletedSuccessfully()
                .assertItem(2);
        s2.assertCompletedSuccessfully()
                .assertItem(3);
    }

    @Test
    public void testWhenTheMapperThrowsAnException() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();

        one.map(v -> {
            throw new RuntimeException("failure");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailure(RuntimeException.class, "failure");
    }

    @Test
    public void testWhenTheMapperThrowsAnError() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();

        one.map(v -> {
            throw new AssertionError("OH NO!");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailure(AssertionError.class, "OH NO!");
    }

    @Test
    public void testThatMapperCanReturnNull() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();

        one.map(v -> null).subscribe().withSubscriber(subscriber);

        subscriber.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testThatMapperIsCalledWithNull() {
        UniAssertSubscriber<String> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().item((String) null).map(x -> "foo").subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedSuccessfully().assertItem("foo");
    }

    @Test
    public void testThatMapperIsCalledOnTheRightExecutor() {
        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            Uni.createFrom().item(1)
                    .emitOn(executor)
                    .map(i -> {
                        threadName.set(Thread.currentThread().getName());
                        return i + 1;
                    })
                    .subscribe().withSubscriber(subscriber);

            subscriber.await().assertCompletedSuccessfully().assertItem(2);
            assertThat(threadName).isNotNull().doesNotHaveValue("main");
            assertThat(subscriber.getOnItemThreadName()).isEqualTo(threadName.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testThatMapperIsNotCalledIfPreviousStageFailed() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().<Integer> failure(new Exception("boom"))
                .map(x -> {
                    called.set(true);
                    return x + 1;
                }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailure(Exception.class, "boom");
        assertThat(called).isFalse();
    }

    @Test
    public void verifyThatTheMapperIsNotCalledAfterCancellationWithEmitter() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom()
                .emitter((Consumer<UniEmitter<? super Integer>>) emitter::set)
                .onItem().transform(i -> {
                    called.set(true);
                    return i + 1;
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertThat(called).isFalse();
        subscriber.assertNotCompleted().assertNoFailure().assertSubscribed();
        subscriber.cancel();
        emitter.get().complete(1);
        assertThat(called).isFalse();
    }

    @Test
    public void verifyThatTheMapperIsNotCalledAfterImmediateCancellationWithEmitter() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom()
                .emitter((Consumer<UniEmitter<? super Integer>>) emitter::set)
                .onItem().transform(i -> {
                    called.set(true);
                    return i + 1;
                })
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        assertThat(called).isFalse();
        subscriber.assertNotCompleted().assertNoFailure().assertSubscribed();
        emitter.get().complete(1);
        assertThat(called).isFalse();
    }
}
