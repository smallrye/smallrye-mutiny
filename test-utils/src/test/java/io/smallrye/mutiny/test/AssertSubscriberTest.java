package io.smallrye.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;

public class AssertSubscriberTest {

    @Test
    public void testItemsAndCompletion() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);
        subscriber.assertNotTerminated();
        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.assertSubscribed();
        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onComplete();

        subscriber.assertItems("a", "b")
                .assertCompleted();
    }

    @Test
    public void testItemsAndFailure() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onError(new IOException("boom"));

        subscriber.assertItems("a", "b")
                .assertFailedWith(IOException.class, "boom")
                .assertTerminated();
    }

    @Test
    public void testNoItems() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        subscriber.assertNotTerminated();
        subscriber.assertHasNotReceivedAnyItem();

        subscriber.cancel();
    }

    @Test
    public void testAwait() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onComplete();
        }).start();

        subscriber.await();
        subscriber.assertCompleted();
    }

    @Test
    public void testAwaitWithDuration() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onComplete();
        }).start();

        subscriber.await(Duration.ofMillis(100));
        subscriber.assertCompleted();
    }

    @Test
    public void testAwaitOnFailure() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onError(new Exception("boom"));
        }).start();

        subscriber.await();
        subscriber.assertFailedWith(Exception.class, "boom");
    }

    @Test
    public void testAwaitAlreadyCompleted() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        subscriber.onComplete();

        subscriber.await();
        subscriber.assertCompleted();
    }

    @Test
    public void testAwaitAlreadyFailed() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        subscriber.onError(new Exception("boom"));

        subscriber.await();
        subscriber.assertFailedWith(Exception.class, "boom");
    }

    @Test
    public void testUpfrontCancellation() {
        AssertSubscriber<String> subscriber = new AssertSubscriber<>(0, true);
        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        verify(subscription).cancel();
    }

    @Test
    public void testUpfrontRequest() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create(10);
        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        verify(subscription).request(10);
    }

    @Test
    public void testRun() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        AtomicInteger count = new AtomicInteger();
        subscriber.run(count::incrementAndGet).run(count::incrementAndGet);

        assertThat(count).hasValue(2);

        assertThatThrownBy(() -> subscriber.run(() -> {
            throw new IllegalStateException("boom");
        }))
                .isInstanceOf(AssertionError.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> subscriber.run(() -> {
            throw new AssertionError("boom");
        })).isInstanceOf(AssertionError.class);
    }

}
