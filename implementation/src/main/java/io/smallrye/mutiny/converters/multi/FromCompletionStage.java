package io.smallrye.mutiny.converters.multi;

import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;

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
