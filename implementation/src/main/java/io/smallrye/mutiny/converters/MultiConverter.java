package io.smallrye.mutiny.converters;

import io.smallrye.mutiny.Multi;

public interface MultiConverter<I, T> {
    /**
     * Convert from type to {@link Multi}.
     *
     * @param instance what is to be converted
     * @return {@link Multi}
     */
    Multi<T> from(I instance);
}
