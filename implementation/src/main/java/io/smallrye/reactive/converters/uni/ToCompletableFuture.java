package io.smallrye.reactive.converters.uni;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.smallrye.reactive.Uni;

public class ToCompletableFuture<T> implements Function<Uni<T>, CompletableFuture<T>> {

    public static final ToCompletableFuture INSTANCE = new ToCompletableFuture();

    private ToCompletableFuture() {
        // Avoid direct instantiation
    }

    @Override
    public CompletableFuture<T> apply(Uni<T> uni) {
        return uni.subscribeAsCompletionStage();
    }
}
