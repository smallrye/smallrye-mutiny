package io.smallrye.mutiny.helpers.test;

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

        assertThatThrownBy(() -> subscriber.assertItems("a", "B"))
                .isInstanceOf(AssertionError.class);

        assertThatThrownBy(subscriber::assertHasNotReceivedAnyItem)
                .isInstanceOf(AssertionError.class);

        assertThatThrownBy(() -> subscriber.assertItems("a", "B", "c"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected <b> to be equal to <B>")
                .hasMessageContaining("Missing expected item <c>");

        assertThatThrownBy(() -> subscriber.assertItems("a"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected the following items to be received");
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
    public void testNotCompleted() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.onNext("a");
        subscriber.onNext("b");

        subscriber.assertItems("a", "b")
                .assertNotTerminated();

        assertThatThrownBy(subscriber::assertCompleted)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion");

        subscriber.onError(new Exception("boom"));

        assertThatThrownBy(subscriber::assertCompleted)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");
    }

    @Test
    public void testNotFailed() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);
        verify(subscription).request(2);
        subscriber.onNext("a");
        subscriber.onNext("b");

        subscriber.assertItems("a", "b")
                .assertNotTerminated();

        assertThatThrownBy(() -> subscriber.assertFailedWith(Exception.class, ""))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");

        subscriber.onComplete();

        assertThatThrownBy(() -> subscriber.assertFailedWith(Exception.class, ""))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("completion event");
    }

    @Test
    public void testNoItems() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        subscriber.request(2);

        subscriber.assertNotTerminated();
        subscriber.assertHasNotReceivedAnyItem();

        assertThatThrownBy(() -> subscriber.assertItems("a"))
                .isInstanceOf(AssertionError.class);

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

        assertThatThrownBy(() -> subscriber.assertFailedWith(IllegalStateException.class, ""))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");

        assertThatThrownBy(() -> subscriber.assertFailedWith(Exception.class, "nope"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("failure");
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

    @Test
    public void testCancelWithoutSubscription() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();
        assertThatThrownBy(subscriber::cancel)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscription");
    }

    @Test
    public void testMultipleSubscriptions() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();

        subscriber.assertNotSubscribed();
        assertThatThrownBy(subscriber::assertSubscribed)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscription");

        subscriber.onSubscribe(mock(Subscription.class));
        assertThatThrownBy(subscriber::assertNotSubscribed)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscription");
        subscriber.assertSubscribed();

        subscriber.onSubscribe(mock(Subscription.class));
        assertThatThrownBy(subscriber::assertNotSubscribed)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscriptions")
                .hasMessageContaining("2");
        assertThatThrownBy(subscriber::assertSubscribed)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("subscriptions")
                .hasMessageContaining("2");
    }

    @Test
    public void testTermination() {
        AssertSubscriber<String> subscriber = AssertSubscriber.create();

        subscriber.assertNotTerminated();
        assertThatThrownBy(subscriber::assertTerminated)
                .isInstanceOf(AssertionError.class);

        subscriber.onComplete();
        assertThatThrownBy(subscriber::assertNotTerminated)
                .isInstanceOf(AssertionError.class);
        subscriber.assertTerminated();
    }

}
