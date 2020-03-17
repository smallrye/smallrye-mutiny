package io.smallrye.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.Subscription;

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

}
