package io.smallrye.mutiny.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class HalfSerializerTest {

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testThatOnNextIsNotReentrant() {
        AtomicInteger wip = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscriber<Object>> subscriber = new AtomicReference<>();
        AssertSubscriber<Object> test = AssertSubscriber.create(10);

        MultiSubscriber s = new MultiSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                test.onSubscribe(s);
            }

            @Override
            public void onItem(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onNext(subscriber.get(), 2, wip, failure);
                }
                test.onNext(t);
            }

            @Override
            public void onFailure(Throwable t) {
                test.onError(t);
            }

            @Override
            public void onCompletion() {
                test.onComplete();
            }
        };

        subscriber.set(s);
        Subscription subscription = mock(Subscription.class);
        s.onSubscribe(subscription);
        HalfSerializer.onNext(s, 1, wip, failure);
        test.assertItems(1).assertFailedWith(IllegalStateException.class, "concurrent onNext(item) signals");
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testThatOnNextAndOnFailureAreNotReentrant() {
        AtomicInteger wip = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscriber<Object>> subscriber = new AtomicReference<>();
        AssertSubscriber<Object> test = AssertSubscriber.create(10);

        MultiSubscriber s = new MultiSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                test.onSubscribe(s);
            }

            @Override
            public void onItem(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onError(subscriber.get(), new IOException("boom"), wip, failure);
                }
                test.onNext(t);
            }

            @Override
            public void onFailure(Throwable t) {
                test.onError(t);
            }

            @Override
            public void onCompletion() {
                test.onComplete();
            }
        };

        subscriber.set(s);
        Subscription subscription = mock(Subscription.class);
        s.onSubscribe(subscription);
        HalfSerializer.onNext(s, 1, wip, failure);
        test.assertFailedWith(IOException.class, "boom");
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testThatOnFailureAndOnFailureAreNotReentrant() {
        AtomicInteger wip = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Subscriber<Object>> subscriber = new AtomicReference<>();
        AssertSubscriber<Object> test = AssertSubscriber.create(10);

        MultiSubscriber s = new MultiSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                test.onSubscribe(s);
            }

            @Override
            public void onItem(Object t) {
                test.onNext(t);
            }

            @Override
            public void onFailure(Throwable t) {
                test.onError(t);
                HalfSerializer.onError(subscriber.get(), new IOException("boom"), wip, failure);
            }

            @Override
            public void onCompletion() {
                test.onComplete();
            }
        };

        subscriber.set(s);
        Subscription subscription = mock(Subscription.class);
        s.onSubscribe(subscription);
        HalfSerializer.onError(s, new IOException("test"), wip, failure);
        test.assertFailedWith(IOException.class, "test");
    }

    @RepeatedTest(100)
    public void testOnNextOnCompleteRace() throws InterruptedException {
        AtomicInteger wip = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Subscription subscription = mock(Subscription.class);
        AssertSubscriber<Object> test = AssertSubscriber.create(10);
        test.onSubscribe(subscription);

        CountDownLatch latch = new CountDownLatch(2);

        Runnable r1 = () -> {
            HalfSerializer.onNext(test, 1, wip, failure);
            latch.countDown();
        };

        Runnable r2 = () -> {
            HalfSerializer.onComplete(test, wip, failure);
            latch.countDown();
        };

        shuffleAndRun(r1, r2);

        latch.await(10, TimeUnit.SECONDS);

        test.assertCompleted();
        assertThat(test.getItems()).hasSizeBetween(0, 1);
    }

    @RepeatedTest(100)
    public void testOnErrorOnCompleteRace() throws InterruptedException {
        AtomicInteger wip = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Subscription subscription = mock(Subscription.class);
        AssertSubscriber<Object> test = AssertSubscriber.create(10);
        test.onSubscribe(subscription);

        CountDownLatch latch = new CountDownLatch(2);

        Runnable r1 = () -> {
            HalfSerializer.onError(test, new IOException("boom"), wip, failure);
            latch.countDown();
        };

        Runnable r2 = () -> {
            HalfSerializer.onComplete(test, wip, failure);
            latch.countDown();
        };

        shuffleAndRun(r1, r2);

        latch.await(10, TimeUnit.SECONDS);
        test.assertTerminated();
    }

    private void shuffleAndRun(Runnable r1, Runnable r2) {
        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(runnable -> new Thread(runnable).start());
    }

}
