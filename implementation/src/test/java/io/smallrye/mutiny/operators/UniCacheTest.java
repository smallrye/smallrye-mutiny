package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.testng.annotations.Test;

import io.reactivex.Flowable;
import io.reactivex.processors.UnicastProcessor;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCacheTest {

    private static void race(Runnable candidate1, Runnable candidate2, Executor s) {
        final CountDownLatch latch = new CountDownLatch(2);

        final RuntimeException[] errors = { null, null };

        List<Runnable> runnables = new ArrayList<>();
        runnables.add(candidate1);
        runnables.add(candidate2);
        Collections.shuffle(runnables);

        s.execute(() -> {
            try {
                runnables.get(0).run();
            } catch (RuntimeException ex) {
                errors[0] = ex;
            } finally {
                latch.countDown();
            }
        });

        s.execute(() -> {
            try {
                runnables.get(1).run();
            } catch (RuntimeException ex) {
                errors[0] = ex;
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("The wait timed out!");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }

        if (errors[0] != null) {
            throw errors[0];
        }

        if (errors[1] != null) {
            throw errors[1];
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "`upstream`.*")
    public void testThatSourceCannotBeNull() {
        new UniCache<>(null, (i, t) -> true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "`validator`.*")
    public void testThatValidatorCannotBeNull() {
        new UniCache<>(Uni.createFrom().item(0), null);
    }

    @Test
    public void testThatImmediateValueAreCached() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> cache = Uni.createFrom().item(counter.incrementAndGet()).cache();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);
        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompletedSuccessfully().assertItem(1);
        sub2.assertCompletedSuccessfully().assertItem(1);
        sub3.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testThatImmediateCachedValuesCanBeRecomputed() {
        AtomicInteger itemValidationCount = new AtomicInteger(0);
        AtomicBoolean isCacheValid = new AtomicBoolean(true);
        BiPredicate<Integer, Throwable> validator = (i, t) -> {
            itemValidationCount.incrementAndGet();
            return isCacheValid.get();
        };
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> cache = Uni.createFrom().item(counter.incrementAndGet())
                .cacheEvents().whilst(validator);

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub4 = UniAssertSubscriber.create();

        // initial value from upstream
        cache.subscribe().withSubscriber(sub1);
        // cached value
        cache.subscribe().withSubscriber(sub2);

        isCacheValid.set(false);
        // recomputed value
        cache.subscribe().withSubscriber(sub3);

        isCacheValid.set(true);
        cache.subscribe().withSubscriber(sub4);
        // item validation count is (subscription count minus upstream subscription count) since only cached items gets validated
        // the initially retrieved from upstream is always accepted
        assertThat(itemValidationCount.get()).isEqualTo(3);

        sub1.assertCompletedSuccessfully().assertItem(1);
        sub2.assertCompletedSuccessfully().assertItem(1);
        sub3.assertCompletedSuccessfully().assertItem(1);
        sub4.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testThatIFailureAreCached() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Object> cache = Uni.createFrom().failure(new Exception("" + counter.getAndIncrement())).cache();

        UniAssertSubscriber<Object> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Object> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Object> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);
        cache.subscribe().withSubscriber(sub3);

        sub1.assertFailure(Exception.class, "0");
        sub2.assertFailure(Exception.class, "0");
        sub3.assertFailure(Exception.class, "0");
    }

    @Test
    public void testThatCachedIFailureCanBeRecomputed() {
        AtomicInteger itemValidationCount = new AtomicInteger(0);
        AtomicBoolean isCacheValid = new AtomicBoolean(true);
        BiPredicate<Object, Throwable> validator = (i, t) -> {
            itemValidationCount.incrementAndGet();
            return isCacheValid.get();
        };
        AtomicInteger counter = new AtomicInteger();
        Uni<Object> cache = Uni.createFrom().failure(new Exception("" + counter.getAndIncrement()))
                .cacheEvents().whilst(validator);

        UniAssertSubscriber<Object> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Object> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Object> sub3 = UniAssertSubscriber.create();
        UniAssertSubscriber<Object> sub4 = UniAssertSubscriber.create();

        // initial value from upstream
        cache.subscribe().withSubscriber(sub1);
        // cached value
        cache.subscribe().withSubscriber(sub2);

        isCacheValid.set(false);
        // recomputed value
        cache.subscribe().withSubscriber(sub3);

        isCacheValid.set(true);
        cache.subscribe().withSubscriber(sub4);
        // item validation count is (subscription count minus upstream subscription count) since only cached items gets validated
        // the initially retrieved from upstream is always accepted
        assertThat(itemValidationCount.get()).isEqualTo(3);

        sub1.assertFailure(Exception.class, "0");
        sub2.assertFailure(Exception.class, "0");
        sub3.assertFailure(Exception.class, "0");
        sub4.assertFailure(Exception.class, "0");
    }

    @Test
    public void testThatValueEmittedAfterSubscriptionAreCached() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).cache();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        cs.complete(1);

        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompletedSuccessfully().assertItem(1);
        sub2.assertCompletedSuccessfully().assertItem(1);
        sub3.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testThatSubscriberCanCancelTheirSubscriptionBeforeReceivingAValue() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).cache();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        sub2.cancel();

        cs.complete(1);

        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompletedSuccessfully().assertItem(1);
        sub2.assertNotCompleted();
        sub3.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testThatSubscriberCanCancelTheirSubscriptionAfterHavingReceivingAValue() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).cache();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        cs.complete(1);
        sub2.cancel();

        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompletedSuccessfully().assertItem(1);
        sub2.assertCompletedSuccessfully().assertItem(1);
        sub3.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void assertCachingTheValueEmittedByAProcessor() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        Uni<Integer> cached = Uni.createFrom().publisher(processor).cache();

        UniAssertSubscriber<Integer> sub1 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub2 = new UniAssertSubscriber<>();

        cached.subscribe().withSubscriber(sub1);
        cached.subscribe().withSubscriber(sub2);

        sub1.assertNotCompleted();
        sub2.assertNotCompleted();

        processor.onNext(23);
        processor.onNext(42);
        processor.onComplete();

        sub1.assertCompletedSuccessfully().assertItem(23);
        sub2.assertCompletedSuccessfully().assertItem(23);
    }

    @Test
    public void assertCancellingImmediately() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        Uni<Integer> cached = Uni.createFrom().publisher(processor).cache();

        UniAssertSubscriber<Integer> sub1 = new UniAssertSubscriber<>(true);
        UniAssertSubscriber<Integer> sub2 = new UniAssertSubscriber<>(true);

        cached.subscribe().withSubscriber(sub1);
        cached.subscribe().withSubscriber(sub2);

        sub1.assertNoResult().assertNoFailure();
        sub2.assertNoResult().assertNoFailure();

        processor.onNext(23);
        processor.onNext(42);
        processor.onComplete();

        sub1.assertNoResult().assertNoFailure();
        sub2.assertNoResult().assertNoFailure();
    }

    @Test
    public void testSubscribersRace() {
        for (int i = 0; i < 2000; i++) {
            Flowable<Integer> flowable = Flowable.just(1, 2, 3);
            Uni<Integer> cached = Uni.createFrom().publisher(flowable).cache();

            UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>(false);

            Runnable r1 = () -> {
                cached.subscribe().withSubscriber(subscriber);
                subscriber.cancel();
            };

            Runnable r2 = () -> cached.subscribe().withSubscriber(new UniAssertSubscriber<>());

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            try {
                race(r1, r2, executor);
            } finally {
                executor.shutdown();
            }
        }
    }

    @Test
    public void testWithDoubleCancellation() {
        Uni<Integer> uni = Uni.createFrom().item(23).cache();
        UniSubscriber<Integer> subscriber = new UniSubscriber<Integer>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                subscription.cancel();
                subscription.cancel();
            }

            @Override
            public void onItem(Integer ignored) {

            }

            @Override
            public void onFailure(Throwable ignored) {

            }
        };
        uni.subscribe().withSubscriber(subscriber);

        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        uni.subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(23);

        uni.subscribe().withSubscriber(subscriber);
    }

    @Test
    public void testThatCacheInvalidationCausesResubscriptionAndCannotRace() {
        AtomicInteger cacheValidationCount = new AtomicInteger(0);
        BiPredicate<Integer, Throwable> validator = (i, f) -> cacheValidationCount.incrementAndGet() % 2 == 0;
        AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
        Uni<Integer> uni = Uni.createFrom().deferred(() -> Uni.createFrom().item(sourceSubscriptionCount.getAndIncrement()))
                .cacheEvents().whilst(validator);

        // Every second subscription causes a re-subscription to the upstream
        // This must not cause race conditions
        List<UniAssertSubscriber<Integer>> results = Collections.synchronizedList(new LinkedList<>());
        Executor parallel = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2000; i++) {
            Runnable subscribe = () -> {
                UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>(false);
                uni.subscribe().withSubscriber(subscriber);
                results.add(subscriber);
            };
            race(subscribe, subscribe, parallel);
        }

        Uni.createFrom().item(0).onItem().delayIt().by(Duration.ofMillis(500)).await().indefinitely();

        // there are two subscriptions in each of the 2000 loop iterations
        // every second of them the cache is invalidated an the UniCaches subscription renewed
        assertThat(sourceSubscriptionCount.get()).isEqualTo(2001);
        // cache validation count is (subscription count (2x2000) - 1) since the initial subscription will not be validated
        assertThat(cacheValidationCount.get()).isEqualTo(3999);
        // every subscriber should have received an item
        results.forEach(UniAssertSubscriber::assertCompletedSuccessfully);
        // every item should be retrieved by at least one subscriber
        Map<Integer, List<Integer>> groupResults = results.stream().map(UniAssertSubscriber::getItem)
                .collect(Collectors.groupingBy(Function.identity()));
        assertThat(groupResults).hasSize(2001);
        Set<Integer> distinctItems = new HashSet<>(groupResults.keySet());
        assertThat(distinctItems).hasSize(2001);
        assertThat(distinctItems)
                .containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(0, 2000).boxed().collect(Collectors.toList()));
    }

    @Test
    public void testThatItemOnlyValidatorResubscribesOnFailure() {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        AtomicBoolean produceFailure = new AtomicBoolean(true);
        Uni uni = Uni.createFrom().deferred(() -> {
            if (produceFailure.getAndSet(false)) {
                return Uni.createFrom().failure(new Exception("" + subscriptionCount.getAndIncrement()));
            } else {
                return Uni.createFrom().item(subscriptionCount.getAndIncrement());
            }
        }).cacheEvents().itemOnly();

        UniAssertSubscriber<Integer> sub1 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub2 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub3 = new UniAssertSubscriber<>();

        uni.subscribe().withSubscriber(sub1);
        uni.subscribe().withSubscriber(sub2);
        uni.subscribe().withSubscriber(sub3);

        assertThat(subscriptionCount.get()).isEqualTo(2);
        sub1.assertFailure(Exception.class, "0");
        sub2.assertItem(1);
        sub3.assertItem(1);
    }

    @Test
    public void testThatItemIsRenewedAfterCacheDurationExceeded() {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        Uni uni = Uni.createFrom().deferred(() -> Uni.createFrom().item(subscriptionCount.getAndIncrement()))
                .cacheEvents().atMost(Duration.ofMillis(100));

        UniAssertSubscriber<Integer> sub1 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub2 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub3 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub4 = new UniAssertSubscriber<>();

        uni.subscribe().withSubscriber(sub1);
        uni.subscribe().withSubscriber(sub2);

        Uni.createFrom().item(0).onItem().delayIt().by(Duration.ofMillis(100)).await().indefinitely();

        uni.subscribe().withSubscriber(sub3);
        uni.subscribe().withSubscriber(sub4);

        assertThat(subscriptionCount.get()).isEqualTo(2);
        sub1.assertItem(0);
        sub2.assertItem(0);
        sub3.assertItem(1);
        sub3.assertItem(1);
    }

    @Test
    public void testThatFailureIsRenewedEvenBeforeCacheDurationExceeded() {
        AtomicInteger subscriptionCount = new AtomicInteger(0);
        Uni uni = Uni.createFrom()
                .deferred(() -> Uni.createFrom().failure(new Exception("" + subscriptionCount.getAndIncrement())))
                .cacheEvents().atMost(Duration.ofMinutes(1));

        UniAssertSubscriber<Integer> sub1 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub2 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub3 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub4 = new UniAssertSubscriber<>();

        uni.subscribe().withSubscriber(sub1);
        uni.subscribe().withSubscriber(sub2);
        uni.subscribe().withSubscriber(sub3);
        uni.subscribe().withSubscriber(sub4);

        assertThat(subscriptionCount.get()).isEqualTo(4);
        sub1.assertFailure(Exception.class, "0");
        sub2.assertFailure(Exception.class, "1");
        sub3.assertFailure(Exception.class, "2");
        sub4.assertFailure(Exception.class, "3");
    }

    @Test
    /*
        TODO: This test is flaky: sometimes it ran into:
        `java.lang.AssertionError: The uni didn't completed successfully: java.lang.IllegalStateException: Invalid transition, expected to be in the HAS_SUBSCRIPTION state but was in [1|2]`
     */
    public void testThatOnlyInvalidCacheEventsDoesNotRace() {
        AtomicInteger cacheValidationCount = new AtomicInteger(0);
        // events are always invalid, every test causes a resubscribe to upstream, since `onItem` is propagated to UniCache subscripte
        BiPredicate<Integer, Throwable> validator = (i, f) -> false;
        // multiple subscriptions may (and will) retrieve a single item from upstream after invalidation, so we cannot verify upstream subscription count
        Uni<Integer> uni = Uni.createFrom().deferred(() -> Uni.createFrom().item(0))
                .cacheEvents().whilst(validator);

        // Every second subscription causes a re-subscription to the upstream
        // This must not cause race conditions
        List<UniAssertSubscriber<Integer>> results = Collections.synchronizedList(new LinkedList<>());
        Executor parallel = Executors.newFixedThreadPool(10);
        Runnable subscribe = () -> {
            UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>(false);
            uni.subscribe().withSubscriber(subscriber);
            results.add(subscriber);
        };
        for (int i = 0; i < 1000; i++) {
            parallel.execute(subscribe);
        }

        Uni.createFrom().item(0).onItem().delayIt().by(Duration.ofMillis(500)).await().indefinitely();

        assertThat(results).hasSize(1000);
        results.forEach(UniAssertSubscriber::assertCompletedSuccessfully);
    }

}
