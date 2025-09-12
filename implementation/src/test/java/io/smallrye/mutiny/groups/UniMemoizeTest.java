package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.reactivex.rxjava3.processors.UnicastProcessor;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.spies.UniOnSubscribeSpy;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.operators.uni.UniMemoizeOp;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import junit5.support.InfrastructureResource;
import mutiny.zero.flow.adapters.AdaptersToFlow;

@DisplayName("Tests for the uni.memoize() group")
@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
class UniMemoizeTest {

    private static void race(Runnable candidate1, Runnable candidate2, Executor s) {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(2);

        final RuntimeException[] errors = { null, null };

        List<Runnable> runnables = new ArrayList<>();
        runnables.add(candidate1);
        runnables.add(candidate2);
        Collections.shuffle(runnables);

        s.execute(() -> {
            try {
                startLatch.await();
                runnables.get(0).run();
            } catch (RuntimeException ex) {
                errors[0] = ex;
            } catch (InterruptedException e) {
                errors[0] = new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        s.execute(() -> {
            try {
                startLatch.await();
                runnables.get(1).run();
            } catch (RuntimeException ex) {
                errors[1] = ex;
            } catch (InterruptedException e) {
                errors[1] = new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        startLatch.countDown();

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

    @Test
    @DisplayName("The upstream cannot be null")
    void testThatSourceCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new UniMemoizeOp<>(null));
    }

    @Test
    @DisplayName("memoize().until(null) is forbidden")
    void testUntilNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).memoize().until(null));
    }

    @Test
    @DisplayName("memoize().forFixedDuration(null) is forbidden")
    void testForFixedDurationNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).memoize().forFixedDuration(null));
    }

    @Test
    @DisplayName("memoize().forFixedDuration(0) is forbidden")
    void testForFixedDurationZero() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).memoize().forFixedDuration(Duration.ZERO));
    }

    @Test
    @DisplayName("memoize().forFixedDuration(negative) is forbidden")
    void testForFixedDurationNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).memoize().forFixedDuration(Duration.ofMillis(-10)));
    }

    @Test
    @DisplayName("Test the deprecated uni.cache() method")
    void testDeprecatedUniCache() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> cache = Uni.createFrom().item(counter.incrementAndGet()).memoize().indefinitely();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);
        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompleted().assertItem(1);
        sub2.assertCompleted().assertItem(1);
        sub3.assertCompleted().assertItem(1);
    }

    @Test
    @DisplayName("Test that uni.memoize().indefinitely() caches immediate values")
    void testThatImmediateValueAreCached() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> cache = Uni.createFrom().item(counter.incrementAndGet()).memoize().indefinitely();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);
        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompleted().assertItem(1);
        sub2.assertCompleted().assertItem(1);
        sub3.assertCompleted().assertItem(1);
    }

    @Test
    @DisplayName("Test that uni.memoize().indefinitely() caches immediate failures")
    void testThatImmediateFailureAreCached() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Object> cache = Uni.createFrom().failure(new Exception("" + counter.getAndIncrement())).memoize()
                .indefinitely();

        UniAssertSubscriber<Object> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Object> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Object> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);
        cache.subscribe().withSubscriber(sub3);

        sub1.assertFailedWith(Exception.class, "0");
        sub2.assertFailedWith(Exception.class, "0");
        sub3.assertFailedWith(Exception.class, "0");
    }

    @Test
    @DisplayName("Test that uni.memoize().indefinitely() caches values emitted after the subscription")
    void testThatValueEmittedAfterSubscriptionAreCached() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).memoize().indefinitely();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        cs.complete(1);

        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompleted().assertItem(1);
        sub2.assertCompleted().assertItem(1);
        sub3.assertCompleted().assertItem(1);
    }

    @Test
    @DisplayName("Test that uni.memoize().indefinitely() subscribers can cancel their subscription before receiving anything")
    void testThatSubscriberCanCancelTheirSubscriptionBeforeReceivingAValue() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).memoize().indefinitely();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        sub2.cancel();

        cs.complete(1);

        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompleted().assertItem(1);
        sub2.assertNotTerminated();
        sub3.assertCompleted().assertItem(1);
    }

    @Test
    @DisplayName("Test that uni.memoize().indefinitely() subscribers can cancel their subscription after having received something")
    void testThatSubscriberCanCancelTheirSubscriptionAfterHavingReceivingAValue() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).memoize().indefinitely();

        UniAssertSubscriber<Integer> sub1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub2 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> sub3 = UniAssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        cs.complete(1);
        sub2.cancel();

        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompleted().assertItem(1);
        sub2.assertCompleted().assertItem(1);
        sub3.assertCompleted().assertItem(1);
    }

    @Test
    @DisplayName("Test that uni.memoize().indefinitely() caches values emitted by a processor")
    void assertCachingTheValueEmittedByAProcessor() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        Uni<Integer> cached = Uni.createFrom().publisher(AdaptersToFlow.publisher(processor)).memoize().indefinitely();

        UniAssertSubscriber<Integer> sub1 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub2 = new UniAssertSubscriber<>();

        cached.subscribe().withSubscriber(sub1);
        cached.subscribe().withSubscriber(sub2);

        sub1.assertNotTerminated();
        sub2.assertNotTerminated();

        processor.onNext(23);
        processor.onNext(42);
        processor.onComplete();

        sub1.assertCompleted().assertItem(23);
        sub2.assertCompleted().assertItem(23);
    }

    @Test
    @DisplayName("Test that uni.cache() caches values emitted by a processor")
    void assertCachingTheValueEmittedByAProcessorUsingDeprecatedUniCache() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        Uni<Integer> cached = Uni.createFrom().publisher(AdaptersToFlow.publisher(processor)).memoize().indefinitely();

        UniAssertSubscriber<Integer> sub1 = new UniAssertSubscriber<>();
        UniAssertSubscriber<Integer> sub2 = new UniAssertSubscriber<>();

        cached.subscribe().withSubscriber(sub1);
        cached.subscribe().withSubscriber(sub2);

        sub1.assertNotTerminated();
        sub2.assertNotTerminated();

        processor.onNext(23);
        processor.onNext(42);
        processor.onComplete();

        sub1.assertCompleted().assertItem(23);
        sub2.assertCompleted().assertItem(23);
    }

    @Test
    @DisplayName("Test that uni.memoize().indefinitely() allows the immediate cancellation when values are emitted by a processor")
    void assertCancellingImmediately() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        Uni<Integer> cached = Uni.createFrom().publisher(AdaptersToFlow.publisher(processor)).memoize().indefinitely();

        UniAssertSubscriber<Integer> sub1 = new UniAssertSubscriber<>(true);
        UniAssertSubscriber<Integer> sub2 = new UniAssertSubscriber<>(true);

        cached.subscribe().withSubscriber(sub1);
        cached.subscribe().withSubscriber(sub2);

        sub1.assertNotTerminated();
        sub2.assertNotTerminated();

        processor.onNext(23);
        processor.onNext(42);
        processor.onComplete();

        sub1.assertNotTerminated();
        sub2.assertNotTerminated();
    }

    @RepeatedTest(10)
    @DisplayName("Test uni.memoize().indefinitely() for race conditions on subscription and cancellation")
    void testSubscribersRace() {
        for (int i = 0; i < 2000; i++) {
            Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
            Uni<Integer> cached = Uni.createFrom().publisher(multi).memoize().indefinitely();

            UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>(false);

            Runnable r1 = () -> {
                cached.subscribe().withSubscriber(subscriber);
                subscriber.cancel();
            };

            Runnable r2 = () -> cached.subscribe().withSubscriber(new UniAssertSubscriber<>());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                race(r1, r2, executor);
            } finally {
                executor.shutdown();
            }
        }
    }

    @Test
    @DisplayName("Test the double cancellation of a subscription to uni.memoize().indefinitely()")
    void testWithDoubleCancellation() {
        Uni<Integer> uni = Uni.createFrom().item(23).memoize().indefinitely();
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

            @Override
            public Context context() {
                return Context.empty();
            }
        };
        uni.subscribe().withSubscriber(subscriber);

        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        uni.subscribe().withSubscriber(test);
        test.assertCompleted().assertItem(23);

        uni.subscribe().withSubscriber(subscriber);
    }

    @Test
    @DisplayName("Test basic invalidations of uni.memoize().until(condition)")
    void testBasicInvalidation() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean invalidate = new AtomicBoolean(false);
        UniOnSubscribeSpy<Integer> onSubscribeSpy = Spy.onSubscribe(Uni.createFrom().item(counter::getAndIncrement));
        Uni<Integer> cachingUni = onSubscribeSpy.memoize().until(invalidate::get);

        UniAssertSubscriber<Integer> subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(0);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(1);

        subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(0);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(1);

        invalidate.set(true);

        subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(2);

        subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(2);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(3);

        invalidate.set(false);

        subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(2);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Test basic invalidations of uni.memoize().during(duration)")
    void testDurationInvalidation() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        UniOnSubscribeSpy<Integer> onSubscribeSpy = Spy.onSubscribe(Uni.createFrom().item(counter::getAndIncrement));
        Uni<Integer> cachingUni = onSubscribeSpy.memoize().forFixedDuration(Duration.ofMillis(250));

        UniAssertSubscriber<Integer> subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(0);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(1);

        subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(0);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(1);

        Thread.sleep(500);

        subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(2);

        subscriber = cachingUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1);
        assertThat(onSubscribeSpy.invocationCount()).isEqualTo(2);
    }

    @RepeatedTest(10)
    @DisplayName("Test uni.memoize().until(duration) for race conditions on subscription and cancellation in presence of random guard invalidations")
    void testSubscribersRaceWithRandomInvalidations() {
        for (int i = 0; i < 2000; i++) {
            Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
            BooleanSupplier invalidationGuard = () -> ThreadLocalRandom.current().nextBoolean();
            Uni<Integer> cached = Uni.createFrom().publisher(multi).memoize().until(invalidationGuard);

            UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>(false);

            Runnable r1 = () -> {
                cached.subscribe().withSubscriber(subscriber);
                subscriber.cancel();
            };

            Runnable r2 = () -> cached.subscribe().withSubscriber(new UniAssertSubscriber<>());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                race(r1, r2, executor);
            } finally {
                executor.shutdown();
            }
        }
    }

    @RepeatedTest(10)
    public void testDrainBlockedByAwait() {
        Uni<Integer> uni = Uni.createFrom().item(() -> 1)
                .memoize().indefinitely();
        assertThat(uni
                .onItem().transform(x -> uni.await().indefinitely())
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem().getItem()).isEqualTo(1);

    }

    /**
     * Test reproducing https://github.com/smallrye/smallrye-mutiny/issues/460
     */
    @RepeatedTest(10)
    public void testTimeoutOfSecondSubscriber() {
        Uni<String> uni = Uni.createFrom().item("hello")
                .onItem().delayIt().by(Duration.ofMillis(500))
                .memoize().indefinitely();

        AtomicReference<String> reference = new AtomicReference<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        Cancellable cancellable = uni
                .onCancellation().invoke(() -> cancelled.set(true))
                .subscribe().with(reference::set);

        uni
                .ifNoItem().after(Duration.ofMillis(100)).fail()
                .onFailure(TimeoutException.class).invoke(failure -> cancellable.cancel())
                .onFailure().recoverWithItem(() -> null)
                .await().indefinitely();

        assertThat(reference).hasValue(null);
        assertThat(cancelled).isTrue();
    }

    @RepeatedTest(3)
    void testMemoizeForever() {
        AtomicInteger count = new AtomicInteger();
        Uni<String> uni = Uni.createFrom().item(() -> {
            int i = count.incrementAndGet();
            return "hello-" + i;
        })
                .onItem().delayIt().by(Duration.ofMillis(500))
                .memoize().forFixedDuration(ChronoUnit.FOREVER.getDuration());
        UniAssertSubscriber<String> subscriber1 = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<String> subscriber2 = uni.subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber1.awaitItem().assertItem("hello-1");
        subscriber2.awaitItem().assertItem("hello-1");
    }

    @Test
    public void reproducer_1303() throws ExecutionException, InterruptedException {
        // See https://github.com/quarkusio/quarkus/issues/33602
        var ex = Executors.newFixedThreadPool(1);
        for (int i = 0; i < 500_000; i++) {
            CompletableFuture<Object> cf = new CompletableFuture<>();
            Uni.createFrom().emitter(emitter -> ex.submit(() -> emitter.complete(new Object())))
                    .memoize().indefinitely()
                    .subscribe().with(cf::complete);
            if (cf.get() == null) {
                throw new RuntimeException();
            }
        }
    }

    @Test
    public void checkProtocolCorrectness() {
        var log = new ArrayList<String>();
        var uni = Uni.createFrom().item(() -> 58).memoize().indefinitely()
                .onSubscription().invoke(() -> log.add("sub"))
                .onItem().invoke(n -> log.add(String.valueOf(n)));

        Integer res = uni.await().atMost(Duration.ofSeconds(5));
        assertThat(res).isEqualTo(58);
        assertThat(log).containsExactly("sub", "58");

        uni.await().atMost(Duration.ofSeconds(5));
        assertThat(res).isEqualTo(58);
        assertThat(log).containsExactly("sub", "58", "sub", "58");
    }

    @RepeatedTest(10)
    void reproducer_1910() {
        // Adapted from https://github.com/smallrye/smallrye-mutiny/issues/1910 (deadlock)
        var executor = ForkJoinPool.commonPool();
        var future = CompletableFuture.failedFuture(new RuntimeException("Woops"));
        var cf = CompletableFuture.supplyAsync(() -> {
            try {
                return future.get(100, TimeUnit.MILLISECONDS);
            } catch (Throwable err) {
                throw new CompletionException(err);
            }
        }, executor);

        var nodesUni = Uni.createFrom().completionStage(cf)
                .memoize().indefinitely();

        var topicListingsUni = nodesUni.map(ignored -> null);

        var combined = Uni
                .combine().all().unis(nodesUni, topicListingsUni).asTuple()
                .memoize().indefinitely();

        var resultFuture = combined.runSubscriptionOn(executor).subscribeAsCompletionStage();
        try {
            resultFuture.get(100, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | InterruptedException | java.util.concurrent.TimeoutException e) {
            // Ok
        } finally {
            resultFuture.cancel(true);
        }
    }

    @RepeatedTest(1000)
    public void testCachingRaceInNotification() {
        AtomicInteger sub = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();

        ExecutorService pool = ForkJoinPool.commonPool();
        Uni<Integer> uni = Uni.createFrom().item(count::incrementAndGet)
                .emitOn(pool)
                .runSubscriptionOn(pool)
                .memoize().until(() -> sub.incrementAndGet() > 2);

        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(1);
        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(1);
        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(2);
        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(3);
        assertThat(uni.await().atMost(Duration.ofMillis(100))).isEqualTo(4);
    }
}
