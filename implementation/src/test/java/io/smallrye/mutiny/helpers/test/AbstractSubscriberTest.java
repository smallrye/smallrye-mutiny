package io.smallrye.mutiny.helpers.test;

import static io.smallrye.mutiny.helpers.test.AssertSubscriber.DEFAULT_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class AbstractSubscriberTest {

    @Test
    public void testOnNext() {
        List<String> items = new ArrayList<>();
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<String>() {
            @Override
            public void onNext(String o) {
                items.add(o);
            }
        };

        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onComplete();
        assertThat(items).containsExactly("a", "b");
    }

    @Test
    public void testOnError() {
        AtomicBoolean called = new AtomicBoolean();
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<String>() {
            @Override
            public void onError(Throwable t) {
                super.onError(t);
                called.set(true);
            }
        };

        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onError(new Exception("boom"));
        assertThat(called).isTrue();
    }

    @Test
    public void testOnComplete() {
        AtomicBoolean called = new AtomicBoolean();
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<String>() {
            @Override
            public void onComplete() {
                called.set(true);
            }
        };

        subscriber.onNext("a");
        subscriber.onNext("b");
        subscriber.onComplete();
        assertThat(called).isTrue();
    }

    @Test
    public void testSubscription() {
        Subscription subscription = mock(Subscription.class);
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<>();

        subscriber.onSubscribe(subscription);
        subscriber.request(10);
        verify(subscription, times(1)).request(10);
    }

    @Test
    public void testSubscriptionUpstream() {
        Subscription subscription = mock(Subscription.class);
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<>(2);

        subscriber.onSubscribe(subscription);
        verify(subscription, times(1)).request(2);
        subscriber.request(10);
        verify(subscription, times(1)).request(10);
        subscriber.cancel();
        verify(subscription, times(1)).cancel();
    }

    @Test
    public void testRequestWithoutSubscription() {
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<>(2);
        assertThatThrownBy(() -> subscriber.request(2)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(subscriber::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testOnSubscribedCalledTwice() {
        Subscription subscription = mock(Subscription.class);
        AbstractSubscriber<String> subscriber = new AbstractSubscriber<>(2);
        subscriber.onSubscribe(subscription);
        assertThatThrownBy(() -> subscriber.onSubscribe(subscription)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testIsCancelledWithUpfrontCancellation() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(10, true);
        assertThat(subscriber.isCancelled()).isFalse();

        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);

        assertThat(subscriber.isCancelled()).isTrue();
        verify(subscription).cancel();
        verify(subscription, never()).request(anyLong());
    }

    @Test
    public void testIsCancelledWithCancellation() {
        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(10, false);
        assertThat(subscriber.isCancelled()).isFalse();

        subscriber.assertNotSubscribed()
                .assertNotTerminated();
        assertThat(subscriber.isCancelled()).isFalse();

        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);

        assertThat(subscriber.isCancelled()).isFalse();
        verify(subscription).request(10);

        subscriber.cancel();
        verify(subscription).cancel();
        assertThat(subscriber.isCancelled()).isTrue();
    }

    @Test
    public void testAwaitWithTimeout() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        assertThatThrownBy(() -> subscriber.awaitFailure(Duration.ofMillis(1))).isInstanceOf(AssertionError.class);

        assertThatThrownBy(() -> await()
                .pollDelay(Duration.ofMillis(1))
                .atMost(Duration.ofMillis(2)).untilAsserted(() -> subscriber.awaitFailure(DEFAULT_TIMEOUT)))
                .isInstanceOf(ConditionTimeoutException.class);

        assertThatThrownBy(() -> await()
                .pollDelay(Duration.ofMillis(1))
                .atMost(Duration.ofMillis(2)).untilAsserted(subscriber::awaitCompletion))
                .isInstanceOf(ConditionTimeoutException.class);
    }

    @Test
    public void testAwaitCompletionWithInterruption() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        AtomicBoolean unblocked = new AtomicBoolean();
        Thread thread = new Thread(() -> {
            subscriber.awaitCompletion(Duration.ofSeconds(100));
            unblocked.set(true);
        });
        thread.start();
        thread.interrupt();

        await().untilTrue(unblocked);

        unblocked.set(false);
        thread = new Thread(() -> {
            subscriber.awaitCompletion();
            unblocked.set(true);
        });
        thread.start();
        thread.interrupt();

        await().untilTrue(unblocked);
    }

    @Test
    @Timeout(1)
    public void testAwaitWhenAlreadyCompleted() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        subscriber.onComplete();

        subscriber.awaitCompletion(Duration.ofSeconds(100));
        subscriber.awaitCompletion();
    }

    @Test
    @Timeout(1)
    public void testAwaitWhenAlreadyFailed() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        subscriber.onError(new IOException("boom"));

        subscriber.awaitFailure(Duration.ofSeconds(100));
        subscriber.awaitFailure();

        subscriber.assertFailedWith(IOException.class, "boom");

    }
}
