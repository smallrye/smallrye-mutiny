package io.smallrye.reactive.adapt;

import io.smallrye.reactive.adapt.converters.FromCompletionStage;

public class BuiltinConverters {
    private BuiltinConverters() {
        // Avoid direct instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> FromCompletionStage<T> fromCompletionStage() {
        return FromCompletionStage.INSTANCE;
    }
}
