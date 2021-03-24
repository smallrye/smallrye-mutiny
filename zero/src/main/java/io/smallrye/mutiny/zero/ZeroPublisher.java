package io.smallrye.mutiny.zero;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.zero.impl.CompletionStagePublisher;
import io.smallrye.mutiny.zero.impl.IterablePublisher;

public interface ZeroPublisher {

    static <T> Publisher<T> fromItems(T... items) {
        requireNonNull(items, "The items array cannot be null");
        return fromIterable(Arrays.asList(items));
    }

    static <T> Publisher<T> fromIterable(Iterable<T> iterable) {
        requireNonNull(iterable, "The iterable cannot be null");
        return new IterablePublisher<>(iterable);
    }

    static <T> Publisher<T> fromCompletionStage(CompletionStage<T> completionStage) {
        requireNonNull(completionStage, "The CompletionStage cannot be null");
        return new CompletionStagePublisher(completionStage);
    }
}
