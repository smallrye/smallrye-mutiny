package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.BuiltinConverters;

public class UniConvert<T> {

    private final Uni<T> upstream;

    public UniConvert(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Transforms this {@link Uni} into a type using the provided converter.
     *
     * @param converter the converter function
     * @return an instance of R
     * @param <R> the result type
     * @throws RuntimeException if the conversion fails.
     */
    public <R> R with(Function<Uni<T>, R> converter) {
        nonNull(converter, "converter");
        return converter.apply(upstream);
    }

    public CompletionStage<T> toCompletionStage() {
        return with(BuiltinConverters.toCompletionStage());
    }

    public CompletableFuture<T> toCompletableFuture() {
        return with(BuiltinConverters.toCompletableFuture());
    }

    public Publisher<T> toPublisher() {
        return with(BuiltinConverters.toPublisher());
    }

}
