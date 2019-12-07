package io.smallrye.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.Subscription;

public class MultiAssertSubscriberTest {

    @Test
    public void testItemsAndCompletion() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);
        subscriber.assertNotTerminated();
        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.assertSubscribed();
        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onComplete();

        subscriber.assertReceived("a", "b")
                .assertCompletedSuccessfully()
                .assertHasNotFailed();
    }

    @Test
    public void testItemsAndFailure() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onError(new IOException("boom"));

        subscriber.assertReceived("a", "b")
                .assertHasFailedWith(IOException.class, "boom")
                .assertTerminated();
    }

    @Test
    public void testNoItems() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        subscriber.assertNotTerminated();
        subscriber.assertHasNotFailed();
        subscriber.assertHasNotReceivedAnyItem();

        subscriber.cancel();
    }

    @Test
    public void testAwait() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onComplete();
        }).start();

        subscriber.await();
        subscriber.assertCompletedSuccessfully();
    }

    @Test
    public void testAwaitWithDuration() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onComplete();
        }).start();

        subscriber.await(Duration.ofMillis(100));
        subscriber.assertCompletedSuccessfully();
    }

    @Test
    public void testAwaitOnFailure() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        new Thread(() -> {
            subscriber.onNext("1");
            subscriber.onNext("2");
            subscriber.onError(new Exception("boom"));
        }).start();

        subscriber.await();
        subscriber.assertHasFailedWith(Exception.class, "boom");
    }

    @Test
    public void testAwaitAlreadyCompleted() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        subscriber.onComplete();

        subscriber.await();
        subscriber.assertCompletedSuccessfully();
    }

    @Test
    public void testAwaitAlreadyFailed() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        subscriber.onError(new Exception("boom"));

        subscriber.await();
        subscriber.assertHasFailedWith(Exception.class, "boom");
    }

    @Test
    public void testUpfrontCancellation() {
        MultiAssertSubscriber<String> subscriber = new MultiAssertSubscriber<>(0, true);
        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        verify(subscription).cancel();
    }

    @Test
    public void testUpfrontRequest() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create(10);
        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        verify(subscription).request(10);
    }

    @Test
    public void testRun() {
        MultiAssertSubscriber<String> subscriber = MultiAssertSubscriber.create();
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
