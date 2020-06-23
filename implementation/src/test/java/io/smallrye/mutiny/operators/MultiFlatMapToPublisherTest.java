package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiFlatMapToPublisherTest {

    @Test
    public void testMapShortcut() {
        Multi.createFrom().items(1, 2)
                .map(i -> i + 1)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(2))
                .assertCompletedSuccessfully()
                .assertReceived(2, 3);
    }

    @Test
    public void testConcatMap() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .concatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testConcatMapWithEmpty() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .concatMap(i -> Multi.createFrom().<Integer> empty())
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test(timeOut = 60000)
    public void testConcatMapWithLotsOfResults() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem()
                .produceMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i))).collectFailures()
                .concatenate()
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();

        int current = 0;
        for (int next : subscriber.items()) {
            assertThat(next).isEqualTo(current + 1);
            current = next;
        }
    }

    @Test(timeOut = 60000)
    public void testConcatMapWithLotsOfResultsAndFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem().produceMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .collectFailures().concatenate()
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();

        int current = 0;
        for (int next : subscriber.items()) {
            assertThat(next).isEqualTo(current + 1);
            current = next;
        }
    }

    @Test(timeOut = 60000)
    public void testConcatMapWithLotsOfResultsAndFailuresAndFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem().producePublisher(
                        i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                            if (i == 99000 || i == 100_000) {
                                throw new IllegalArgumentException("boom");
                            } else {
                                return i;
                            }
                        })))
                .collectFailures().concatenate()
                .subscribe(subscriber);

        subscriber
                .await()
                .assertHasFailedWith(CompositeException.class, "boom");

        assertThat(subscriber.items().size()).isEqualTo(100_000 - 2);
        int current = 0;
        for (int next : subscriber.items()) {
            assertThat(next).isGreaterThan(current);
            current = next;
        }
    }

    @Test(timeOut = 60000)
    public void testConcatMapWithLotsOfResultsAndFailuresWithoutFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .concatMap(
                        i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                            if (i == 99000) {
                                throw new IllegalArgumentException("boom");
                            } else {
                                return i;
                            }
                        })))
                .subscribe(subscriber);

        subscriber
                .await()
                .assertHasFailedWith(IllegalArgumentException.class, "boom");

        assertThat(subscriber.items().size()).isEqualTo(99000 - 1);
        int current = 0;
        for (int next : subscriber.items()) {
            assertThat(next).isGreaterThan(current);
            current = next;
        }
    }

    @Test
    public void testConcatMapWithDelayOfFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().producePublisher(i -> Multi.createFrom().items(i, i)).collectFailures().concatenate()
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testConcatMapWithFailuresAndDelay() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().produceMulti(i -> {
                    if (i == 2) {
                        return Multi.createFrom().failure(new IOException("boom"));
                    } else {
                        return Multi.createFrom().items(i, i);
                    }
                }).collectFailures().concatenate()
                .subscribe(subscriber);

        subscriber
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 1, 3, 3);
    }

    @Test
    public void testRegularFlatMap() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testFlatMapShortcut() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testThatFlatMapIsNotCalledOnUpstreamFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .flatMap(i -> {
                    count.incrementAndGet();
                    return Multi.createFrom().item(i);
                })
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IOException.class, "boom");
        assertThat(count).hasValue(0);
    }

    @Test
    public void testThatFlatMapIsOnlyCallOnItems() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().<Integer> empty()
                .flatMap(i -> {
                    count.incrementAndGet();
                    return Multi.createFrom().item(i);
                })
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        assertThat(count).hasValue(0);
    }

    @Test
    public void testRegularFlatMapWithRequests() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        Multi.createFrom().range(1, 4)
                .flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber
                .assertSubscribed()
                .assertHasNotReceivedAnyItem();

        subscriber.request(2)
                .run(() -> assertThat(subscriber.items()).hasSize(2))
                .request(2)
                .run(() -> assertThat(subscriber.items()).hasSize(4))
                .request(10)
                .run(() -> assertThat(subscriber.items()).hasSize(6))
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).contains(1, 1, 2, 2, 3, 3);
    }

    @Test
    public void testFlatMapWithMapperThrowingException() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .<Integer> flatMap(i -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testFlatMapWithMapperReturningNull() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .<Integer> flatMap(i -> null)
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testFlatMapWithMapperReturningNullInAMulti() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .<Integer> flatMap(i -> Multi.createFrom().item(null))
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IllegalArgumentException.class, "supplier");
    }

    @Test
    public void testFlatMapWithMapperProducingFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().range(1, 4)
                .<Integer> flatMap(i -> Multi.createFrom().failure(new IOException("boom")))
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IOException.class, "boom");
        assertThat(count).hasValue(0);
    }

    @Test
    public void testFlatMapWithABitMoreResults() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 2001)
                .flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(4000);
    }

    @Test
    public void testFlatMapWithConcurrency() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10001)
                .onItem().produceMulti(i -> Multi.createFrom().items(i, i)).merge(25)
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(20000);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFlatMapWithInvalidConcurrency() {
        Multi.createFrom().range(1, 10001)
                .onItem().produceMulti(i -> Multi.createFrom().items(i, i))
                .merge(-1);
    }

    @Test
    public void testFlatMapWithConcurrencyAndAsyncEmission() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10001)
                .onItem().produceMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .withRequests(20)
                .merge(25)
                .subscribe(subscriber);

        subscriber.await().assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(10000);
    }

    @Test
    public void testRegularFlatMapWithFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 5)
                .onItem().produceMulti(i -> {
                    if (i % 2 == 0) {
                        return Multi.createFrom().failure(new IOException("boom"));
                    } else {
                        return Multi.createFrom().item(i);
                    }
                })
                .collectFailures()
                .merge()
                .subscribe(subscriber);

        subscriber.assertReceived(1, 3).assertHasFailedWith(CompositeException.class, "boom");
    }

    @Test
    public void testFlatMapUni() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().produceUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).hasSize(3).contains(2, 3, 4);
    }

    @Test
    public void testFlatMapCompletionStage() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().produceCompletionStage(i -> CompletableFuture.supplyAsync(() -> i + 1))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).hasSize(3).contains(2, 3, 4);
    }

    @Test
    public void testConcatMapUni() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().produceUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .concatenate()
                .collectItems().asList().await().indefinitely();

        assertThat(list).hasSize(3).containsExactly(2, 3, 4);
    }

    @Test
    public void testFlatMapIterable() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().produceIterable(i -> Arrays.asList(i, i + 1))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).hasSize(6).containsExactlyInAnyOrder(1, 2, 2, 3, 3, 4);
    }

    @Test
    public void testConcatMapIterable() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().produceIterable(i -> Arrays.asList(i, i + 1))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).hasSize(6).containsExactly(1, 2, 2, 3, 3, 4);
    }

    @Test
    public void testThatUpstreamFailureCancelledInnersAndIsPropagated() {
        UnicastProcessor<Integer> processor1 = UnicastProcessor.create();
        UnicastProcessor<Integer> processor2 = UnicastProcessor.create();

        MultiAssertSubscriber<Integer> subscriber = processor1
                .flatMap(x -> processor2)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        processor1.onNext(1);
        assertTrue(processor2.hasSubscriber());
        processor1.onError(new IOException("boom"));
        assertFalse(processor2.hasSubscriber());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testThatUpstreamIsCancelledWhenInnerFails() {
        UnicastProcessor<Integer> processor1 = UnicastProcessor.create();
        UnicastProcessor<Integer> processor2 = UnicastProcessor.create();

        MultiAssertSubscriber<Integer> subscriber = processor1
                .flatMap(x -> processor2)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        processor1.onNext(1);
        assertTrue(processor2.hasSubscriber());
        processor2.onError(new IOException("boom"));
        assertFalse(processor1.hasSubscriber());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

}
