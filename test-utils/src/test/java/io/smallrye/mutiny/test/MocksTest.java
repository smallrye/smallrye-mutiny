package io.smallrye.mutiny.test;

import static org.mockito.Mockito.*;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

import org.junit.jupiter.api.Test;

public class MocksTest {

    @Test
    public void subscriber() {
        Subscription subscription = mock(Subscription.class);
        Flow.Subscriber<Object> subscriber = Mocks.subscriber();
        subscriber.onSubscribe(subscription);
        verify(subscription).request(Long.MAX_VALUE);
    }

    @Test
    public void subscriberWithRequests() {
        Subscription subscription = mock(Subscription.class);
        Flow.Subscriber<Object> subscriber = Mocks.subscriber(20);
        subscriber.onSubscribe(subscription);
        verify(subscription).request(20);
    }

    @Test
    public void subscriberWithZeroRequest() {
        Subscription subscription = mock(Subscription.class);
        Flow.Subscriber<Object> subscriber = Mocks.subscriber(0);
        subscriber.onSubscribe(subscription);
        verify(subscription, never()).request(anyLong());
    }

}
