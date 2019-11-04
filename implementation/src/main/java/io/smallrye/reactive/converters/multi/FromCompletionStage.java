package io.smallrye.reactive.converters.multi;

import java.util.concurrent.CompletionStage;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.converters.MultiConverter;

public class FromCompletionStage<T> implements MultiConverter<CompletionStage<T>, T> {

    public static final FromCompletionStage INSTANCE = new FromCompletionStage();

    private FromCompletionStage() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(CompletionStage<T> instance) {
        return Multi.createFrom().completionStage(instance);
    }
}
