package io.smallrye.mutiny.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.mockito.Mockito;

public class Mocks {

    /**
     * Mocks a subscriber and prepares it to request {@code Long.MAX_VALUE}.
     *
     * @param <T> the value type
     * @return the mocked subscriber
     */
    public static <T> Subscriber<T> subscriber() {
        return subscriber(Long.MAX_VALUE);
    }

    /**
     * Mocks a subscriber and prepares it to request {@code req}.
     *
     * @param <T> the value type
     * @return the mocked subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> Subscriber<T> subscriber(long req) {
        Subscriber<T> subscriber = mock(Subscriber.class);
        Mockito.doAnswer(invocation -> {
            Subscription subscription = invocation.getArgument(0, Subscription.class);
            if (req != 0) {
                subscription.request(req);
            }
            return null;
        }).when(subscriber).onSubscribe(any(Subscription.class));
        return subscriber;
    }
}
