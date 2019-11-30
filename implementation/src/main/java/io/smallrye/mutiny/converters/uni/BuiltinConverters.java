package io.smallrye.mutiny.converters.uni;

public class BuiltinConverters {
    private BuiltinConverters() {
        // Avoid direct instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> FromCompletionStage<T> fromCompletionStage() {
        return FromCompletionStage.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToCompletionStage<T> toCompletionStage() {
        return ToCompletionStage.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToCompletableFuture<T> toCompletableFuture() {
        return ToCompletableFuture.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToPublisher<T> toPublisher() {
        return ToPublisher.INSTANCE;
    }
}
