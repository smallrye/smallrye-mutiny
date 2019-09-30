package io.smallrye.reactive.unimulti.adapt.converters;

import io.smallrye.reactive.unimulti.Uni;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ToCompletableFuture<T> implements Function<Uni<T>, CompletableFuture<T>> {
    @Override
    public CompletableFuture<T> apply(Uni<T> uni) {
        return uni.subscribeAsCompletionStage();
    }
}
