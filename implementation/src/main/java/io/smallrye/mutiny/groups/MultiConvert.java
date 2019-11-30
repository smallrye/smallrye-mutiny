package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiConvert<T> {

    private final Multi<T> upstream;

    public MultiConvert(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Transforms this {@link Multi} into a type using the provided converter.
     *
     * @param converter the converter function
     * @return an instance of R
     * @throws RuntimeException if the conversion fails.
     */
    public <R> R with(Function<Multi<T>, R> converter) {
        nonNull(converter, "converter");
        return converter.apply(upstream);
    }

    public Publisher<T> toPublisher() {
        return upstream;
    }
}
