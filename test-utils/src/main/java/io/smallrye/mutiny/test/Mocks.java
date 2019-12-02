package io.smallrye.mutiny.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Mocks {

    /**
     * Mocks a subscriber and prepares it to request {@code Long.MAX_VALUE}.
     * 
     * @param <T> the value type
     * @return the mocked subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> Subscriber<T> subscriber() {
        Subscriber<T> w = mock(Subscriber.class);

        Mockito.doAnswer((Answer<Object>) a -> {
            Subscription s = a.getArgument(0);
            s.request(Long.MAX_VALUE);
            return null;
        }).when(w).onSubscribe(any());

        return w;
    }

}
