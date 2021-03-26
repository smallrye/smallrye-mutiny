package io.smallrye.mutiny.zero;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.zero.impl.CompletionStagePublisher;
import io.smallrye.mutiny.zero.impl.FailurePublisher;
import io.smallrye.mutiny.zero.impl.IterablePublisher;
import io.smallrye.mutiny.zero.impl.StreamPublisher;

public interface ZeroPublisher {

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

    static <T> Publisher<T> fromCompletionStage(CompletionStage<T> completionStage) {
        requireNonNull(completionStage, "The CompletionStage cannot be null");
        return new CompletionStagePublisher<>(completionStage);
    }

    static <T> Publisher<T> fromFailure(Throwable failure) {
        requireNonNull(failure, "The failure cannot be null");
        return new FailurePublisher<>(failure);
    }
}
