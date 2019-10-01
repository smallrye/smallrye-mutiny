package io.smallrye.reactive.adapt.converters;

import java.util.concurrent.CompletionStage;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniConverter;

public class FromCompletionStage<T> implements UniConverter<CompletionStage<T>, T> {
    @Override
    public Uni<T> from(CompletionStage<T> instance) {
        return Uni.createFrom().completionStage(instance);
    }
}
