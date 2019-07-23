package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.reactivex.processors.UnicastProcessor;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UniCacheTest {


    @Test(expected = IllegalArgumentException.class)
    public void testThatSourceCannotBeNull() {
        new UniCache<>(null);
    }


    @Test
    public void testThatImmediateValueAreCached() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> cache = Uni.createFrom().result(counter.incrementAndGet()).cache();

        AssertSubscriber<Integer> sub1 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub2 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub3 = AssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);
        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompletedSuccessfully().assertResult(1);
        sub2.assertCompletedSuccessfully().assertResult(1);
        sub3.assertCompletedSuccessfully().assertResult(1);
    }

    @Test
    public void testThatIFailureAreCached() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Object> cache = Uni.createFrom().failure(new Exception("" + counter.getAndIncrement())).cache();

        AssertSubscriber<Object> sub1 = AssertSubscriber.create();
        AssertSubscriber<Object> sub2 = AssertSubscriber.create();
        AssertSubscriber<Object> sub3 = AssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);
        cache.subscribe().withSubscriber(sub3);

        sub1.assertFailure(Exception.class, "0");
        sub2.assertFailure(Exception.class, "0");
        sub3.assertFailure(Exception.class, "0");
    }

    @Test
    public void testThatValueEmittedAfterSubscriptionAreCached() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).cache();

        AssertSubscriber<Integer> sub1 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub2 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub3 = AssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        cs.complete(1);

        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompletedSuccessfully().assertResult(1);
        sub2.assertCompletedSuccessfully().assertResult(1);
        sub3.assertCompletedSuccessfully().assertResult(1);
    }

    @Test
    public void testThatSubscriberCanCancelTheirSubscriptionBeforeReceivingAValue() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).cache();

        AssertSubscriber<Integer> sub1 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub2 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub3 = AssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        sub2.cancel();

        cs.complete(1);

        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompletedSuccessfully().assertResult(1);
        sub2.assertNotCompleted();
        sub3.assertCompletedSuccessfully().assertResult(1);
    }

    @Test
    public void testThatSubscriberCanCancelTheirSubscriptionAfterHavingReceivingAValue() {
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> cache = Uni.createFrom().completionStage(cs).cache();

        AssertSubscriber<Integer> sub1 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub2 = AssertSubscriber.create();
        AssertSubscriber<Integer> sub3 = AssertSubscriber.create();

        cache.subscribe().withSubscriber(sub1);
        cache.subscribe().withSubscriber(sub2);

        cs.complete(1);
        sub2.cancel();


        cache.subscribe().withSubscriber(sub3);

        sub1.assertCompletedSuccessfully().assertResult(1);
        sub2.assertCompletedSuccessfully().assertResult(1);
        sub3.assertCompletedSuccessfully().assertResult(1);
    }


    @Test
    public void assertCachingTheValueEmittedByAProcessor() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        Uni<Integer> cached = Uni.createFrom().publisher(processor).cache();

        AssertSubscriber<Integer> sub1 = new AssertSubscriber<>();
        AssertSubscriber<Integer> sub2 = new AssertSubscriber<>();

        cached.subscribe().withSubscriber(sub1);
        cached.subscribe().withSubscriber(sub2);

        sub1.assertNotCompleted();
        sub2.assertNotCompleted();

        processor.onNext(23);
        processor.onNext(42);
        processor.onComplete();

        sub1.assertCompletedSuccessfully().assertResult(23);
        sub2.assertCompletedSuccessfully().assertResult(23);
    }

    @Test
    public void assertCancellingImmediately() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        Uni<Integer> cached = Uni.createFrom().publisher(processor).cache();

        AssertSubscriber<Integer> sub1 = new AssertSubscriber<>(true);
        AssertSubscriber<Integer> sub2 = new AssertSubscriber<>(true);

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

            AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(false);

            Runnable r1 = () -> {
                cached.subscribe().withSubscriber(subscriber);
                subscriber.cancel();
            };

            Runnable r2 = () -> cached.subscribe().withSubscriber(new AssertSubscriber<>());

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            try {
                race(r1, r2, executor);
            } finally {
                executor.shutdown();
            }
        }
    }

    private static void race(Runnable candidate1, Runnable candidate2, Executor s) {
        final CountDownLatch latch = new CountDownLatch(2);

        final RuntimeException[] errors = {null, null};


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

    @Test
    public void testWithDoubleCancellation() {
        Uni<Integer> uni = Uni.createFrom().result(23).cache();
        UniSubscriber<Integer> subscriber = new UniSubscriber<Integer>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                subscription.cancel();
                subscription.cancel();
            }

            @Override
            public void onResult(Integer result) {

            }

            @Override
            public void onFailure(Throwable failure) {

            }
        };
        uni.subscribe().withSubscriber(subscriber);

        AssertSubscriber<Integer> test = AssertSubscriber.create();
        uni.subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertResult(23);

        uni.subscribe().withSubscriber(subscriber);
    }

}