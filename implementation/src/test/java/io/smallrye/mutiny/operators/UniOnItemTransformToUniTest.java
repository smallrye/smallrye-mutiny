package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

@SuppressWarnings("ConstantConditions")
public class UniOnItemTransformToUniTest {

    @Test
    public void testTransformToUniWithImmediateValue() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).onItem().transformToUni(v -> Uni.createFrom().item(2)).subscribe()
                .withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceUniWithImmediateValueDeprecated() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).onItem().produceUni(v -> Uni.createFrom().item(2)).subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    public void testTransformToUniShortcutFlatmap() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).flatMap(v -> Uni.createFrom().item(2)).subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    public void testTransformToUniShortcutChain() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).chain(v -> Uni.createFrom().item(2)).subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    public void testTransformToUniShortcutThen() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).then(() -> Uni.createFrom().item(2)).subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    public void testTransformToUniShortcutThenWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).chain((Supplier<Uni<?>>) null));
    }

    @Test
    public void testTransformToUniShortcutChainWithNullMapper() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).chain((Function<? super Integer, Uni<?>>) null));
    }

    @Test
    public void testWithImmediateCancellation() {
        UniAssertSubscriber<Integer> test = new UniAssertSubscriber<>(true);
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().transformToUni(v -> {
            called.set(true);
            return Uni.createFrom().item(2);
        }).subscribe().withSubscriber(test);
        test.assertNotCompleted();
        assertThat(called).isFalse();
    }

    @Test
    public void testWithADeferredUi() {
        UniAssertSubscriber<Integer> test1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> test2 = UniAssertSubscriber.create();
        AtomicInteger count = new AtomicInteger(2);
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni(v -> Uni.createFrom().deferred(() -> Uni.createFrom().item(count.incrementAndGet())));
        uni.subscribe().withSubscriber(test1);
        uni.subscribe().withSubscriber(test2);
        test1.assertCompletedSuccessfully().assertItem(3).assertNoFailure();
        test2.assertCompletedSuccessfully().assertItem(4).assertNoFailure();
    }

    @Test
    public void testWithAnUniResolvedAsynchronously() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni(
                        v -> Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(42)).start()));
        uni.subscribe().withSubscriber(test);
        test.await().assertCompletedSuccessfully().assertItem(42).assertNoFailure();
    }

    @Test
    public void testWithAnUniResolvedAsynchronouslyWithAFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem().transformToUni(v -> Uni.createFrom()
                .emitter(emitter -> new Thread(() -> emitter.fail(new IOException("boom"))).start()));
        uni.subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testThatMapperIsNotCalledOnUpstreamFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().failure(new Exception("boom")).onItem().transformToUni(v -> {
            called.set(true);
            return Uni.createFrom().item(2);
        }).subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(Exception.class, "boom");
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
        test.await().assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
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
        test.await().assertCompletedWithFailure().assertFailure(NullPointerException.class, "");
        assertThat(called).isTrue();
    }

    @Test
    public void testThatTheMapperCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onItem().transformToUni((Function<Integer, Uni<?>>) null));
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

        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni(v -> Uni.createFrom().completionStage(future));
        uni.subscribe().withSubscriber(test);
        test.cancel();
        test.assertNotCompleted();
        assertThat(cancelled).isTrue();
    }
}
