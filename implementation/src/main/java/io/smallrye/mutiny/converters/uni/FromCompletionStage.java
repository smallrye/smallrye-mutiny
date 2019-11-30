package io.smallrye.mutiny.converters.uni;

import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;

public class FromCompletionStage<T> implements UniConverter<CompletionStage<T>, T> {

    public static final FromCompletionStage INSTANCE = new FromCompletionStage();

    private FromCompletionStage() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(CompletionStage<T> instance) {
        return Uni.createFrom().completionStage(instance);
    }
}
