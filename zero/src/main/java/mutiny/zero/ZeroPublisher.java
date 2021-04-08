package mutiny.zero;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import mutiny.zero.internal.*;

/**
 * Factory methods to simplify the creation of reactive streams compliant {@link org.reactivestreams.Publisher}.
 * <p>
 * There are convenience methods for creating {@link org.reactivestreams.Publisher} from in-memory data.
 * <p>
 * The general-purpose abstraction is to use a {@link Tube} and the {@link #create(BackpressureStrategy, int, Consumer)}
 * factory method.
 */
public interface ZeroPublisher {

    // ---- "Iterate over something" ---- //

    /**
     * Create a {@link org.reactivestreams.Publisher} from existing items.
     *
     * @param items the existing items, cannot be a {@code null array}
     * @param <T> the items type
     * @return a new {@link org.reactivestreams.Publisher}
     */
    @SafeVarargs
    static <T> Publisher<T> fromItems(T... items) {
        requireNonNull(items, "The items array cannot be null");
        return fromIterable(Arrays.asList(items));
    }

    /**
     * Create a {@link org.reactivestreams.Publisher} from an iterable object.
     * <p>
     * Note that this assumes an in-memory, non-blocking {@link java.util.Iterator}.
     * Do not try to force an iterator as a way to bridge an API with {@link org.reactivestreams.Publisher} if it is
     * does not behave like an in-memory data structure.
     *
     * @param iterable the iterable object, cannot be {@code null}
     * @param <T> the items type
     * @return a nes {@link org.reactivestreams.Publisher}
     */
    static <T> Publisher<T> fromIterable(Iterable<T> iterable) {
        requireNonNull(iterable, "The iterable cannot be null");
        return new IterablePublisher<>(iterable);
    }

    /**
     * Create a {@link org.reactivestreams.Publisher} from a {@link java.util.stream.Stream}.
     * <p>
     * Note that this assumes an in-memory, non-blocking data structure, just like {@link #fromIterable(Iterable)}.
     * Also note that a {@link java.util.stream.Stream} can only be traversed once, hence the use of a supplier because
     * multiple subscriptions would fail.
     * 
     * @param supplier the stream supplier, cannot be {@code null}
     * @param <T> the items type
     * @return a new {@link org.reactivestreams.Publisher}
     */
    static <T> Publisher<T> fromStream(Supplier<Stream<T>> supplier) {
        requireNonNull(supplier, "The supplier cannot be null");
        return new StreamPublisher<>(supplier);
    }

    /**
     * Create a {@link org.reactivestreams.Publisher} from a generator over some state.
     * <p>
     * Note that this assumes an in-memory, non-blocking data structure, just like {@link #fromIterable(Iterable)}.
     *
     * @param stateSupplier the initial state supplier, cannot be {@code null} but can supply {@code null}
     * @param generator a generator function over the initial state and an iterator, cannot be {@code null}, cannot yield
     *        {@code null}
     * @param <S> the initial state type
     * @param <T> the items type
     * @return a new {@link org.reactivestreams.Publisher}
     */
    static <S, T> Publisher<T> fromGenerator(Supplier<S> stateSupplier, Function<S, Iterator<T>> generator) {
        requireNonNull(stateSupplier, "The state supplier cannot be null");
        requireNonNull(generator, "The generator supplier cannot be null");
        return new GeneratorPublisher<>(stateSupplier, generator);
    }

    // ---- CompletionStage integration ---- //

    /**
     * Create a {@link org.reactivestreams.Publisher} from a {@link CompletionStage}.
     * 
     * @param completionStage the completion stage, cannot be {@code null}
     * @param <T> the item type
     * @return a new {@link org.reactivestreams.Publisher}
     */
    static <T> Publisher<T> fromCompletionStage(CompletionStage<T> completionStage) {
        requireNonNull(completionStage, "The CompletionStage cannot be null");
        return new CompletionStagePublisher<>(completionStage);
    }

    /**
     * Create a {@link CompletionStage} from a {@link Publisher}.
     * <p>
     * The {@link Publisher} is requested exactly 1 element and the subscription is cancelled after it has been received.
     * 
     * @param publisher the publisher, cannot be {@code null}
     * @param <T> the item type
     * @return a new {@link CompletionStage}
     */
    static <T> CompletionStage<Optional<T>> toCompletionStage(Publisher<T> publisher) {
        requireNonNull(publisher, "The publisher cannot be null");
        CompletableFuture<Optional<T>> future = new CompletableFuture<>();
        publisher.subscribe(new PublisherToCompletionStageSubscriber<>(future));
        return future;
    }

    // ---- Special cases ---- //

    /**
     * Create a {@link org.reactivestreams.Publisher} from a known failure.
     * 
     * @param failure the failure, cannot be {@code null}
     * @param <T> the items type
     * @return a new {@link org.reactivestreams.Publisher}
     */
    static <T> Publisher<T> fromFailure(Throwable failure) {
        requireNonNull(failure, "The failure cannot be null");
        return new FailurePublisher<>(failure);
    }

    /**
     * Create an empty {@link org.reactivestreams.Publisher} that completes upon subscription without ever sending any item.
     * 
     * @param <T> the items type
     * @return a new {@link org.reactivestreams.Publisher}
     */
    static <T> Publisher<T> empty() {
        return new EmptyPublisher<>();
    }

    // ---- Tube / DIY ---- //

    /**
     * Create a new {@link org.reactivestreams.Publisher} with the general-purpose {@link Tube} API.
     * 
     * @param backpressureStrategy the back-pressure strategy, cannot be {@code null}
     * @param bufferSize the buffer size, must be strictly positive when {@code backpressureStrategy} is one of
     *        {@link BackpressureStrategy#BUFFER} and {@link BackpressureStrategy#LATEST}
     * @param tubeConsumer the tube consumer, cannot be {@code null}
     * @param <T> the items type
     * @return a new {@link org.reactivestreams.Publisher}
     */
    static <T> Publisher<T> create(BackpressureStrategy backpressureStrategy, int bufferSize, Consumer<Tube<T>> tubeConsumer) {
        requireNonNull(backpressureStrategy, "The backpressure strategy cannot be null");
        requireNonNull(tubeConsumer, "The tube consumer cannot be null");
        if (((backpressureStrategy == BackpressureStrategy.BUFFER)
                || (backpressureStrategy == BackpressureStrategy.LATEST)) && bufferSize <= 0) {
            throw new IllegalArgumentException("The buffer size must be strictly positive");
        }
        return new TubePublisher<>(backpressureStrategy, bufferSize, tubeConsumer);
    }
}
