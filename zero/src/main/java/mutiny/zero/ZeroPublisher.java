package mutiny.zero;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import mutiny.zero.internal.*;

public interface ZeroPublisher {

    // ---- "Iterate over something" ---- //

    @SafeVarargs
    static <T> Publisher<T> fromItems(T... items) {
        requireNonNull(items, "The items array cannot be null");
        return fromIterable(Arrays.asList(items));
    }

    static <T> Publisher<T> fromIterable(Iterable<T> iterable) {
        requireNonNull(iterable, "The iterable cannot be null");
        return new IterablePublisher<>(iterable);
    }

    static <T> Publisher<T> fromStream(Supplier<Stream<T>> supplier) {
        requireNonNull(supplier, "The supplier cannot be null");
        return new StreamPublisher<>(supplier);
    }

    static <S, T> Publisher<T> fromGenerator(Supplier<S> stateSupplier, Function<S, Iterator<T>> generator) {
        requireNonNull(stateSupplier, "The state supplier cannot be null");
        requireNonNull(generator, "The generator supplier cannot be null");
        return new GeneratorPublisher<>(stateSupplier, generator);
    }

    // ---- CompletionStage integration ---- //

    static <T> Publisher<T> fromCompletionStage(CompletionStage<T> completionStage) {
        requireNonNull(completionStage, "The CompletionStage cannot be null");
        return new CompletionStagePublisher<>(completionStage);
    }

    // ---- Special cases ---- //

    static <T> Publisher<T> fromFailure(Throwable failure) {
        requireNonNull(failure, "The failure cannot be null");
        return new FailurePublisher<>(failure);
    }

    static <T> Publisher<T> empty() {
        return new EmptyPublisher<>();
    }
}
