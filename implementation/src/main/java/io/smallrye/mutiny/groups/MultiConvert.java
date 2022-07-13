package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;

/**
 * Converts the upstream into another reactive type.
 *
 * @param <T> the type of item emitted by the upstream.
 */
public class MultiConvert<T> {

    private final Multi<T> upstream;

    public MultiConvert(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Transforms this {@link Multi} into a type using the provided converter.
     *
     * @param converter the converter function
     * @param <R> the type produced by the converter
     * @return an instance of R
     * @throws RuntimeException if the conversion fails.
     */
    public <R> R with(Function<Multi<T>, R> converter) {
        // No interception for converters.
        nonNull(converter, "converter");
        return converter.apply(upstream);
    }

    /**
     * Converts the {@link Multi} into a {@link Publisher}.
     * <p>
     * Basically, this method returns the {@link Multi} as it is, as {@link Multi} implements {@link Publisher}.
     *
     * @return the publisher
     */
    @CheckReturnValue
    public Publisher<T> toPublisher() {
        return upstream;
    }
}
