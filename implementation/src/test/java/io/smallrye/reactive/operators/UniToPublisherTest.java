package io.smallrye.reactive.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.reactive.Multi;
import org.junit.After;
import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import io.smallrye.reactive.Uni;

public class UniToPublisherTest {

    private ExecutorService executor;

    @After
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    public void testWithImmediateValue() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        int first = Flowable.fromPublisher(publisher).blockingFirst();
        assertThat(first).isEqualTo(1);
    }

    @Test
    public void testWithImmediateNullValue() {
        Publisher<Integer> publisher = Uni.createFrom().item((Integer) null).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        int first = Flowable.fromPublisher(publisher).blockingFirst(2);
        assertThat(first).isEqualTo(2);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testWithImmediateFailure() {
        Publisher<Integer> publisher = Uni.createFrom().<Integer> failure(new IOException("boom")).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        try {
            Flowable.fromPublisher(publisher).blockingFirst();
            fail("exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        }

    }

    @Test
    public void testWithImmediateValueWithRequest() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(publisher).test(0);
        test.assertSubscribed();
        test.request(1);
        test.assertResult(1);
        test.assertComplete();
    }

    @Test
    public void testWithImmediateValueWithRequests() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(publisher).test(0);
        test.assertSubscribed();
        test.request(20);
        test.assertResult(1);
        test.assertComplete();
    }

    @Test
    public void testInvalidRequest() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(publisher).test(0);
        test.assertSubscribed();
        test.request(0);
        test.assertError(IllegalArgumentException.class);
        test.assertTerminated();
    }

    @Test
    public void testWithImmediateValueWithOneRequestAndImmediateCancellation() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(publisher).test(1, true);
        test.assertSubscribed();
        assertThat(test.isCancelled()).isTrue();
        test.assertNotTerminated();
        test.assertNever(1);
    }

    @Test
    public void testThatTwoSubscribersHaveTwoSubscriptions() {
        AtomicInteger count = new AtomicInteger(1);
        Publisher<Integer> publisher = Uni.createFrom().deferred(() -> Uni.createFrom()
                .item(count.getAndIncrement()))
                .adapt().toPublisher();
        assertThat(publisher).isNotNull();
        Flowable<Integer> flow = Flowable.fromPublisher(publisher);
        int first = flow.blockingFirst();
        assertThat(first).isEqualTo(1);
        first = flow.blockingFirst();
        assertThat(first).isEqualTo(2);
    }

    @Test
    public void testThatTwoSubscribersWithCache() {
        AtomicInteger count = new AtomicInteger(1);
        Publisher<Integer> publisher = Uni.createFrom()
                .deferred(() -> Uni.createFrom().item(count.getAndIncrement())).cache().adapt().toPublisher();
        assertThat(publisher).isNotNull();
        Flowable<Integer> flow = Flowable.fromPublisher(publisher);
        int first = flow.blockingFirst();
        assertThat(first).isEqualTo(1);
        first = flow.blockingFirst();
        assertThat(first).isEqualTo(1);
    }

    @Test
    public void testCancellationBetweenSubscriptionAndRequest() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(publisher).test(0);
        test.assertSubscribed();
        test.cancel();
        assertThat(test.isCancelled()).isTrue();
        test.assertNotTerminated();
        test.assertNever(1);
    }

    @Test
    public void testCancellationBetweenRequestAndValue() {
        // TODO This is a very broken implementation of "delay" - to be replace once delay is implemented
        executor = Executors.newSingleThreadExecutor();
        Publisher<Integer> publisher = Uni.createFrom().item(1).emitOn(executor).map(x -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return x;
        }).adapt().toPublisher();

        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(publisher).test(0);
        test.assertSubscribed();
        test.request(1);
        test.cancel();
        assertThat(test.isCancelled()).isTrue();
        test.assertNotTerminated();
        test.assertNever(1);
    }

    @Test
    public void testCancellationAfterValue() {
        Publisher<Integer> publisher = Uni.createFrom().item(1).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        TestSubscriber<Integer> test = Flowable.fromPublisher(publisher).test(0);
        test.assertSubscribed();
        test.request(1);
        // Immediate emission, so cancel is called after the emission.
        test.cancel();
        assertThat(test.isCancelled()).isTrue();
        test.assertValue(1);
        test.assertComplete();
    }

    @Test
    public void testWithAsyncValue() {
        executor = Executors.newSingleThreadScheduledExecutor();
        Publisher<Integer> publisher = Uni.createFrom().item(1).emitOn(executor).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        int first = Flowable.fromPublisher(publisher).blockingFirst();
        assertThat(first).isEqualTo(1);
    }

    @Test
    public void testWithAsyncNullValue() {
        executor = Executors.newSingleThreadScheduledExecutor();

        Publisher<Integer> publisher = Uni.createFrom().item((Integer) null)
                .emitOn(executor)
                .adapt().toPublisher();
        assertThat(publisher).isNotNull();
        int first = Flowable.fromPublisher(publisher).blockingFirst(2);
        assertThat(first).isEqualTo(2);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testWithAsyncFailure() {
        executor = Executors.newSingleThreadScheduledExecutor();
        Publisher<Integer> publisher = Uni.createFrom().<Integer> failure(new IOException("boom"))
                .emitOn(executor).adapt().toPublisher();
        assertThat(publisher).isNotNull();
        try {
            Flowable.fromPublisher(publisher).blockingFirst();
            fail("exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        }
    }

    @Test
    public void testUniOfVoid() {
        Uni<Void> uni = Uni.createFrom().item(null);
        Multi<Void> publisher = uni.toMulti();
        assertThat(publisher.collect().asList().await().indefinitely()).isEmpty();
    }
}
