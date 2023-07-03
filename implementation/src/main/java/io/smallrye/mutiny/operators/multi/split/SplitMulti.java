package io.smallrye.mutiny.operators.multi.split;

import io.smallrye.mutiny.Multi;

/**
 * A {@link Multi} that corresponds to {@link MultiSplitter} key.
 *
 * @param <T> the element types
 * @param <K> the key
 */
public interface SplitMulti<T, K extends Enum<K>> extends Multi<T> {

    /**
     * Get the corresponding split key.
     *
     * @return the key
     */
    K splitKey();
}
