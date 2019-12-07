package io.smallrye.mutiny.test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MocksTest {

    @Test
    public void subscriber() {
        Subscription subscription = mock(Subscription.class);
        Subscriber<Object> subscriber = Mocks.subscriber();
        subscriber.onSubscribe(subscription);
        verify(subscription).request(Long.MAX_VALUE);
    }

}
