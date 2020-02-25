package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class UniOnItemFlatMapTest {

    @Test
    public void testFlatMapWithImmediateValue() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).onItem().produceUni(v -> Uni.createFrom().item(2)).subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    public void testFlatMapShortcut() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).flatMap(v -> Uni.createFrom().item(2)).subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    public void testWithImmediateCancellation() {
        UniAssertSubscriber<Integer> test = new UniAssertSubscriber<>(true);
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().produceUni(v -> {
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
                .produceUni(v -> Uni.createFrom().deferred(() -> Uni.createFrom().item(count.incrementAndGet())));
        uni.subscribe().withSubscriber(test1);
        uni.subscribe().withSubscriber(test2);
        test1.assertCompletedSuccessfully().assertItem(3).assertNoFailure();
        test2.assertCompletedSuccessfully().assertItem(4).assertNoFailure();
    }

    @Test
    public void testWithAnUniResolvedAsynchronously() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .produceUni(v -> Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(42)).start()));
        uni.subscribe().withSubscriber(test);
        test.await().assertCompletedSuccessfully().assertItem(42).assertNoFailure();
    }

    @Test
    public void testWithAnUniResolvedAsynchronouslyWithAFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem().produceUni(v -> Uni.createFrom()
                .emitter(emitter -> new Thread(() -> emitter.fail(new IOException("boom"))).start()));
        uni.subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testThatMapperIsNotCalledOnUpstreamFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().failure(new Exception("boom")).onItem().produceUni(v -> {
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
                .onItem().<Integer> produceUni(v -> {
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
                .onItem().<Integer> produceUni(v -> {
                    called.set(true);
                    return null;
                }).subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(NullPointerException.class, "");
        assertThat(called).isTrue();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheMapperCannotBeNull() {
        Uni.createFrom().item(1).onItem().produceUni((Function<Integer, Uni<?>>) null);
    }

    @Test
    public void testWithCancellationBeforeEmission() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean cancelled = new AtomicBoolean();
        @SuppressWarnings("unchecked")
        CompletableFuture<Integer> future = new CompletableFuture() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                cancelled.set(true);
                return true;
            }
        };

        Uni<Integer> uni = Uni.createFrom().item(1).onItem().produceUni(v -> Uni.createFrom().completionStage(future));
        uni.subscribe().withSubscriber(test);
        test.cancel();
        test.assertNotCompleted();
        assertThat(cancelled).isTrue();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFlatMapMultiWithNullMapper() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        uni.onItem().produceMulti(null);
    }

    @Test
    public void testFlatMapMultiWithItem() {
        Uni.createFrom().item(1)
                .onItem().produceMulti(i -> Multi.createFrom().range(i, 5))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testFlatMapMultiWithNull() {
        Uni.createFrom().voidItem()
                .onItem().produceMulti(x -> Multi.createFrom().range(1, 5))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testFlatMapMultiWithFailure() {
        Uni.createFrom().<Integer> failure(new IOException("boom"))
                .onItem().produceMulti(x -> Multi.createFrom().range(1, 5))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testFlatMapMultiWithExceptionThrownByMapper() {
        Uni.createFrom().item(1)
                .onItem().produceMulti(x -> {
                    throw new IllegalStateException("boom");
                })
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertHasFailedWith(IllegalStateException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testFlatMapMultiWithNullReturnedByMapper() {
        Uni.createFrom().item(1)
                .onItem().produceMulti(x -> null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertHasFailedWith(NullPointerException.class, "")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testFlatMapMultiWithNullReturnedByMapperWithCancellationDuringTheUniResolution() {
        final AtomicBoolean called = new AtomicBoolean();

        Uni.createFrom().<Integer> nothing()
                .on().cancellation(() -> called.set(true))
                .onItem().produceMulti(x -> Multi.createFrom().range(x, 10))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))

                .assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .run(() -> assertThat(called).isFalse())
                .cancel()
                .run(() -> assertThat(called).isTrue())
                .assertNotTerminated();
    }

    @Test
    public void testFlatMapMultiWithNullReturnedByMapperWithCancellationDuringTheMultiEmissions() {
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean calledUni = new AtomicBoolean();

        Uni.createFrom().item(1)
                .on().cancellation(() -> calledUni.set(true))
                .onItem().produceMulti(i -> Multi.createFrom().nothing()
                        .on().cancellation(() -> called.set(true)))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .run(() -> assertThat(called).isFalse())
                .cancel()
                .run(() -> assertThat(called).isTrue())
                .run(() -> assertThat(calledUni).isFalse())
                .assertNotTerminated();
    }
}
