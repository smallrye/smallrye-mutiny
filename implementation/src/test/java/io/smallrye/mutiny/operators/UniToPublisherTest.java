package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

public class UniToPublisherTest {

    private ExecutorService executor;

    @AfterEach
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    public void testWithImmediateValue() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).convert().toPublisher();
        assertThat(publisher).isNotNull();
        int first = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).blockingFirst();
        assertThat(first).isEqualTo(1);
    }

    @Test
    public void testWithImmediateNullValue() {
        Publisher<Integer> publisher = Uni.createFrom().item((Integer) null).convert().toPublisher();
        assertThat(publisher).isNotNull();
        int first = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).blockingFirst(2);
        assertThat(first).isEqualTo(2);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testWithImmediateFailure() {
        Publisher<Integer> publisher = Uni.createFrom().<Integer> failure(new IOException("boom")).convert().toPublisher();
        assertThat(publisher).isNotNull();
        try {
            Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).blockingFirst();
            fail("exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        }

    }

    @Test
    public void testWithImmediateValueWithRequest() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).convert().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).test(0);
        assertThat(test.hasSubscription()).isTrue();
        test.request(1);
        test.assertResult(1);
        test.assertComplete();
    }

    @Test
    public void testWithImmediateValueWithRequests() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).convert().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).test(0);
        assertThat(test.hasSubscription()).isTrue();
        test.request(20);
        test.assertResult(1);
        test.assertComplete();
    }

    @Test
    public void testInvalidRequest() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).convert().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).test(0);
        assertThat(test.hasSubscription()).isTrue();
        test.request(0);
        test.assertError(IllegalArgumentException.class);
        test.assertNotComplete();
    }

    @Test
    public void testWithImmediateValueWithOneRequestAndImmediateCancellation() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).convert().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).test(1, true);
        assertThat(test.hasSubscription()).isTrue();
        assertThat(test.isCancelled()).isTrue();
        test.assertNotComplete();
        test.assertNoValues();
    }

    @Test
    public void testThatTwoSubscribersHaveTwoSubscriptions() {
        AtomicInteger count = new AtomicInteger(1);
        Publisher<Integer> publisher = Uni.createFrom().deferred(() -> Uni.createFrom()
                .item(count.getAndIncrement()))
                .convert().toPublisher();
        assertThat(publisher).isNotNull();
        Flowable<Integer> flow = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher));
        int first = flow.blockingFirst();
        assertThat(first).isEqualTo(1);
        first = flow.blockingFirst();
        assertThat(first).isEqualTo(2);
    }

    @Test
    public void testThatTwoSubscribersWithCache() {
        AtomicInteger count = new AtomicInteger(1);
        Publisher<Integer> publisher = Uni.createFrom()
                .deferred(() -> Uni.createFrom().item(count.getAndIncrement())).memoize().indefinitely().convert()
                .toPublisher();
        assertThat(publisher).isNotNull();
        Flowable<Integer> flow = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher));
        int first = flow.blockingFirst();
        assertThat(first).isEqualTo(1);
        first = flow.blockingFirst();
        assertThat(first).isEqualTo(1);
    }

    @Test
    public void testCancellationBetweenSubscriptionAndRequest() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).convert().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).test(0);
        assertThat(test.hasSubscription()).isTrue();
        test.cancel();
        assertThat(test.isCancelled()).isTrue();
        test.assertNotComplete();
        test.assertNoValues();
    }

    @Test
    public void testCancellationBetweenRequestAndValue() {
        // TODO This is a very broken implementation of "delay" - to be replace once delay is implemented
        executor = Executors.newFixedThreadPool(1);
        Publisher<Integer> publisher = Uni.createFrom().item(1).emitOn(executor).map(x -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return x;
        }).convert().toPublisher();

        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).test(0);
        assertThat(test.hasSubscription()).isTrue();
        test.request(1);
        test.cancel();
        assertThat(test.isCancelled()).isTrue();
        test.assertNotComplete();
        test.assertNoValues();
    }

    @Test
    public void testCancellationAfterValue() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).convert().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).test(0);
        assertThat(test.hasSubscription()).isTrue();
        test.request(1);
        // Immediate emission, so cancel is called after the emission.
        test.cancel();
        assertThat(test.isCancelled()).isTrue();
        test.assertValue(1);
        test.assertComplete();
    }

    @Test
    public void testWithAsyncValue() {
        executor = Executors.newScheduledThreadPool(1);
        Publisher<Integer> publisher = Uni.createFrom().item(1).emitOn(executor).convert().toPublisher();
        assertThat(publisher).isNotNull();
        int first = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).blockingFirst();
        assertThat(first).isEqualTo(1);
    }

    @Test
    public void testWithAsyncNullValue() {
        executor = Executors.newScheduledThreadPool(1);

        Publisher<Integer> publisher = Uni.createFrom().item((Integer) null)
                .emitOn(executor)
                .convert().toPublisher();
        assertThat(publisher).isNotNull();
        int first = Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).blockingFirst(2);
        assertThat(first).isEqualTo(2);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testWithAsyncFailure() {
        executor = Executors.newScheduledThreadPool(1);
        Publisher<Integer> publisher = Uni.createFrom().<Integer> failure(new IOException("boom"))
                .emitOn(executor).convert().toPublisher();
        assertThat(publisher).isNotNull();
        try {
            Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(publisher)).blockingFirst();
            fail("exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        }
    }

    @Test
    public void testUniOfVoid() {
        Uni<Void> uni = Uni.createFrom().voidItem();
        Multi<Void> publisher = uni.toMulti();
        assertThat(publisher.collect().asList().await().indefinitely()).isEmpty();
    }

    @RepeatedTest(1000)
    public void multipleConcurrentRequests() {
        final int n = 8;
        CountDownLatch start = new CountDownLatch(n);

        Multi<Integer> multi = Uni.createFrom()
                .completionStage(() -> CompletableFuture.supplyAsync(() -> 63))
                .toMulti();
        AssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create());

        for (int i = 0; i < n; i++) {
            ForkJoinPool.commonPool().execute(() -> {
                try {
                    start.await();
                    subscriber.request(10L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
            start.countDown();
        }

        subscriber.awaitCompletion();
        assertThat(subscriber.getItems()).hasSize(1).containsExactly(63);
    }
}
