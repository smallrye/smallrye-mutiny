package io.smallrye.reactive.adapt.converters;

import io.smallrye.reactive.Uni;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class ToCompletionStage<T> implements Function<Uni<T>, CompletionStage<T>> {
    @Override
    public CompletionStage<T> apply(Uni<T> uni) {
        return uni.subscribeAsCompletionStage();
    }
}
