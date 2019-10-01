package io.smallrye.reactive.adapt.converters;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.smallrye.reactive.Uni;

public class ToCompletableFuture<T> implements Function<Uni<T>, CompletableFuture<T>> {
    @Override
    public CompletableFuture<T> apply(Uni<T> uni) {
        return uni.subscribeAsCompletionStage();
    }
}
