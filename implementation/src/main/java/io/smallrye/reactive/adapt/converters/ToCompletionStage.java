package io.smallrye.reactive.adapt.converters;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.smallrye.reactive.Uni;

public class ToCompletionStage<T> implements Function<Uni<T>, CompletionStage<T>> {
    @Override
    public CompletionStage<T> apply(Uni<T> uni) {
        return uni.subscribeAsCompletionStage();
    }
}
