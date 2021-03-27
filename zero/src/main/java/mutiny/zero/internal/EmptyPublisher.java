package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class EmptyPublisher<T> implements Publisher<T> {

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        subscriber.onSubscribe(new AlreadyCompletedSubscription());
        subscriber.onComplete();
    }
}
