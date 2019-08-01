package io.smallrye.reactive.operators;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Multi;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiFlatMapToPublisherTest {

    @Test
    public void testConcatMap() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onResult().concatMap(i -> Multi.createFrom().results(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testConcatMapWithEmpty() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onResult().concatMap(i -> Multi.createFrom().<Integer>empty())
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully().assertHasNoResults();
    }

    @Test(timeout = 60000)
    public void testConcatMapWithLotsOfResults() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onResult().concatMap().delayFailureUntilCompletion()
                .mapToMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();

        int current = 0;
        for (int next : subscriber.results()) {
            assertThat(next).isEqualTo(current + 1);
            current = next;
        }
    }

    @Test(timeout = 60000)
    public void testConcatMapWithLotsOfResultsAndFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onResult().concatMap()
                .delayFailureUntilCompletion()
                .mapToMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();

        int current = 0;
        for (int next : subscriber.results()) {
            assertThat(next).isEqualTo(current + 1);
            current = next;
        }
    }

    @Test(timeout = 60000)
    public void testConcatMapWithLotsOfResultsAndFailuresAndFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onResult()
                .concatMap().delayFailureUntilCompletion()
                .mapToMulti(
                        i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                            if (i == 99000 || i == 100_000) {
                                throw new IllegalArgumentException("boom");
                            } else {
                                return i;
                            }
                        }))
                )
                .subscribe(subscriber);

        subscriber
                .await()
                .assertHasFailedWith(CompositeException.class, "boom");

        assertThat(subscriber.results().size()).isEqualTo(100_000 - 2);
        int current = 0;
        for (int next : subscriber.results()) {
            assertThat(next).isGreaterThan(current);
            current = next;
        }
    }

    @Test(timeout = 60000)
    public void testConcatMapWithLotsOfResultsAndFailuresWithoutFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onResult().concatMap(
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

        assertThat(subscriber.results().size()).isEqualTo(99000 - 1);
        int current = 0;
        for (int next : subscriber.results()) {
            assertThat(next).isGreaterThan(current);
            current = next;
        }
    }

    @Test
    public void testConcatMapWithDelayOfFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onResult().concatMap().delayFailureUntilCompletion().mapToMulti(i -> Multi.createFrom().results(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testConcatMapWithFailuresAndDelay() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onResult().concatMap().delayFailureUntilCompletion().mapToMulti(i -> {
            if (i == 2) {
                return Multi.createFrom().failure(new IOException("boom"));
            } else {
                return Multi.createFrom().results(i, i);
            }
        })
                .subscribe(subscriber);

        subscriber
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 1, 3, 3);
    }

    @Test
    public void testRegularFlatMap() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onResult().flatMap(i -> Multi.createFrom().results(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testThatFlatMapIsNotCalledOnUpstreamFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onResult().flatMap(i -> {
            count.incrementAndGet();
            return Multi.createFrom().result(i);
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
                .onResult().flatMap(i -> {
            count.incrementAndGet();
            return Multi.createFrom().result(i);
        })
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully().assertHasNoResults();
        assertThat(count).hasValue(0);
    }

    @Test
    public void testRegularFlatMapWithRequests() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        Multi.createFrom().range(1, 4)
                .onResult().flatMap(i -> Multi.createFrom().results(i, i))
                .subscribe(subscriber);

        subscriber
                .assertSubscribed()
                .assertHasNoResults();

        subscriber.request(2)
                .assertReceived(1, 1)
                .request(2)
                .assertReceived(1, 1, 2, 2)
                .request(10)
                .assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testFlatMapWithMapperThrowingException() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .onResult().<Integer>flatMap(i -> {
            throw new IllegalArgumentException("boom");
        })
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testFlatMapWithMapperReturningNull() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .onResult().<Integer>flatMap(i -> null)
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testFlatMapWithMapperReturningNullInAMulti() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .onResult().<Integer>flatMap(i -> Multi.createFrom().result(null))
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IllegalArgumentException.class, "supplier");
    }

    @Test
    public void testFlatMapWithMapperProducingFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().range(1, 4)
                .onResult().<Integer>flatMap(i -> Multi.createFrom().failure(new IOException("boom")))
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IOException.class, "boom");
        assertThat(count).hasValue(0);
    }

    @Test
    public void testFlatMapWithABitMoreResults() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 2001)
                .onResult().flatMap(i -> Multi.createFrom().results(i, i))
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.results()).hasSize(4000);
    }

    @Test
    public void testFlatMapWithConcurrency() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10001)
                .onResult().flatMap().withConcurrency(20).mapToMulti(i -> Multi.createFrom().results(i, i))
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.results()).hasSize(20000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFlatMapWithInvalidConcurrency() {
        Multi.createFrom().range(1, 10001)
                .onResult().flatMap().withConcurrency(-1).mapToMulti(i -> Multi.createFrom().results(i, i));
    }

    @Test
    public void testFlatMapWithConcurrencyAndAsyncEmission() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10001)
                .onResult().flatMap().withConcurrency(20).mapToMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .subscribe(subscriber);

        subscriber.await().assertCompletedSuccessfully();
        assertThat(subscriber.results()).hasSize(10000);
    }

    @Test
    public void testRegularFlatMapWithFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 5)
                .onResult().flatMap().delayFailureUntilCompletion().mapToMulti(i -> {
            if (i % 2 == 0) {
                return Multi.createFrom().failure(new IOException("boom"));
            } else {
                return Multi.createFrom().result(i);
            }
        })
                .subscribe(subscriber);

        subscriber.assertReceived(1, 3).assertHasFailedWith(CompositeException.class, "boom");
    }

}
