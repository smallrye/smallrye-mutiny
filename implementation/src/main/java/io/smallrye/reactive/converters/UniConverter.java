package io.smallrye.reactive.converters;

import io.smallrye.reactive.Uni;

public interface UniConverter<I, T> {
    /**
     * Convert from type to {@link Uni}.
     *
     * @param instance what is to be converted
     * @return {@link Uni}
     */
    Uni<T> from(I instance);
}
