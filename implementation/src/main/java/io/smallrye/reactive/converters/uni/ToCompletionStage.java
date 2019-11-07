package io.smallrye.reactive.converters.uni;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.smallrye.reactive.Uni;

public class ToCompletionStage<T> implements Function<Uni<T>, CompletionStage<T>> {

    public static final ToCompletionStage INSTANCE = new ToCompletionStage();

    private ToCompletionStage() {
        // Avoid direct instantiation
    }

    @Override
    public CompletionStage<T> apply(Uni<T> uni) {
        return uni.subscribeAsCompletionStage();
    }
}
