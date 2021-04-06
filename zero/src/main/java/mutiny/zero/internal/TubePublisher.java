package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import mutiny.zero.BackpressureStrategy;
import mutiny.zero.Tube;

public class TubePublisher<T> implements Publisher<T> {

    private final BackpressureStrategy backpressureStrategy;
    private final int bufferSize;
    private final Consumer<Tube<T>> tubeConsumer;

    public TubePublisher(BackpressureStrategy backpressureStrategy, int bufferSize, Consumer<Tube<T>> tubeConsumer) {
        this.backpressureStrategy = backpressureStrategy;
        this.bufferSize = bufferSize;
        this.tubeConsumer = tubeConsumer;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        TubeBase<T> tube = null;
        switch (backpressureStrategy) {
            case BUFFER:
                tube = new BufferingTube<>(subscriber, bufferSize);
                break;
            case DROP:
                tube = new DroppingTube<>(subscriber);
                break;
            case ERROR:
                tube = new ErroringTube<>(subscriber);
                break;
            case IGNORE:
                tube = new IgnoringTube<>(subscriber);
                break;
            case LATEST:
                break;
            default:
                throw new IllegalStateException("Unexpected backpressure strategy: " + backpressureStrategy);
        }
        subscriber.onSubscribe(tube);
        tubeConsumer.accept(tube);
    }
}
