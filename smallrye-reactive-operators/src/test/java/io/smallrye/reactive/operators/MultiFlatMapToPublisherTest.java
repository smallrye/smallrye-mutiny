package io.smallrye.reactive.operators;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiFlatMapToPublisherTest {

    @Test
    public void testConcatMap() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().concatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testConcatMapWithEmpty() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().concatMap(i -> Multi.createFrom().<Integer>empty())
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test(timeout = 60000)
    public void testConcatMapWithLotsOfResults() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem()
                .flatMap()
                .multi(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .collectFailures()
                .concatenateResults()
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

    @Test(timeout = 60000)
    public void testConcatMapWithLotsOfResultsAndFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem()
                .flatMap()
                .multi(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .collectFailures()
                .concatenateResults()
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

    @Test(timeout = 60000)
    public void testConcatMapWithLotsOfResultsAndFailuresAndFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem()
                .flatMap()
                .publisher(
                        i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                            if (i == 99000 || i == 100_000) {
                                throw new IllegalArgumentException("boom");
                            } else {
                                return i;
                            }
                        }))
                )
                .collectFailures()
                .concatenateResults()
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

    @Test(timeout = 60000)
    public void testConcatMapWithLotsOfResultsAndFailuresWithoutFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem().concatMap(
                i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                    if (i == 99000) {
                        throw new IllegalArgumentException("boom");
                    } else {
                        return i;
                    }
                }))
        )
                .subscribe(subscriber);

        subscriber
                .await()
                .assertHasFailedWith(CompletionException.class, "boom");

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
                .onItem()
                .flatMap()
                .publisher(i -> Multi.createFrom().items(i, i))
                .collectFailures()
                .concatenateResults()
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testConcatMapWithFailuresAndDelay() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem()
                .flatMap()
                .multi(i -> {
                    if (i == 2) {
                        return Multi.createFrom().failure(new IOException("boom"));
                    } else {
                        return Multi.createFrom().items(i, i);
                    }
                })
                .collectFailures()
                .concatenateResults()
                .subscribe(subscriber);

        subscriber
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 1, 3, 3);
    }

    @Test
    public void testRegularFlatMap() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testThatFlatMapIsNotCalledOnUpstreamFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onItem().flatMap(i -> {
            count.incrementAndGet();
            return Multi.createFrom().item(i);
        })
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IOException.class, "boom");
        assertThat(count).hasValue(0);
    }

    @Test
    public void testThatFlatMapIsOnlyCallOnResults() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().<Integer>empty()
                .onItem().flatMap(i -> {
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
                .onItem().flatMap(i -> Multi.createFrom().items(i, i))
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
                .onItem().<Integer>flatMap(i -> {
            throw new IllegalArgumentException("boom");
        })
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testFlatMapWithMapperReturningNull() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .onItem().<Integer>flatMap(i -> null)
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testFlatMapWithMapperReturningNullInAMulti() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .onItem().<Integer>flatMap(i -> Multi.createFrom().deferredItem(null))
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IllegalArgumentException.class, "supplier");
    }

    @Test
    public void testFlatMapWithMapperProducingFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().range(1, 4)
                .onItem().<Integer>flatMap(i -> Multi.createFrom().failure(new IOException("boom")))
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IOException.class, "boom");
        assertThat(count).hasValue(0);
    }

    @Test
    public void testFlatMapWithABitMoreResults() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 2001)
                .onItem().flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(4000);
    }

    @Test
    public void testFlatMapWithConcurrency() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10001)
                .onItem().flatMap()
                .multi(i -> Multi.createFrom().items(i, i))
                .mergeResults(25)
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(20000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFlatMapWithInvalidConcurrency() {
        Multi.createFrom().range(1, 10001)
                .onItem().flatMap()
                .multi(i -> Multi.createFrom().items(i, i))
                .mergeResults(-1);
    }

    @Test
    public void testFlatMapWithConcurrencyAndAsyncEmission() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10001)
                .onItem().flatMap()
                .multi(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .withRequests(20)
                .mergeResults(25)
                .subscribe(subscriber);


        subscriber.await().assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(10000);
    }

    @Test
    public void testRegularFlatMapWithFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 5)
                .onItem().flatMap()
                .multi(i -> {
                    if (i % 2 == 0) {
                        return Multi.createFrom().failure(new IOException("boom"));
                    } else {
                        return Multi.createFrom().item(i);
                    }
                })
                .collectFailures()
                .mergeResults()
                .subscribe(subscriber);

        subscriber.assertReceived(1, 3).assertHasFailedWith(CompositeException.class, "boom");
    }

    @Test
    public void testFlatMapUni() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .flatMap()
                .unis(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .mergeResults()
                .collect().asList().await().indefinitely();

        assertThat(list).hasSize(3).contains(2, 3, 4);
    }

    @Test
    public void testConcatMapUni() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .flatMap()
                .unis(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .concatenateResults()
                .collect().asList().await().indefinitely();

        assertThat(list).hasSize(3).containsExactly(2, 3, 4);
    }

    @Test
    public void testFlatMapIterable() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .flatMap()
                .iterable(i -> Arrays.asList(i, i + 1))
                .mergeResults()
                .collect().asList().await().indefinitely();

        assertThat(list).hasSize(6).containsExactlyInAnyOrder(1, 2, 2, 3, 3, 4);
    }

    @Test
    public void testConcatMapIterable() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .flatMap()
                .iterable(i -> Arrays.asList(i, i + 1))
                .mergeResults()
                .collect().asList().await().indefinitely();

        assertThat(list).hasSize(6).containsExactly(1, 2, 2, 3, 3, 4);
    }

}
