package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class GeneratorPublisher<S, T> implements Publisher<T> {

    private final Supplier<S> stateSupplier;
    private final Function<S, Iterator<T>> generator;

    public GeneratorPublisher(Supplier<S> stateSupplier, Function<S, Iterator<T>> generator) {
        this.stateSupplier = stateSupplier;
        this.generator = generator;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        Iterator<T> iterator = generator.apply(stateSupplier.get());
        if (iterator == null) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            subscriber.onError(new IllegalArgumentException("The generator cannot produce a null iterator"));
        } else if (!iterator.hasNext()) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            subscriber.onComplete();
        } else {
            subscriber.onSubscribe(new IteratorSubscription<>(iterator, subscriber));
        }
    }

}
