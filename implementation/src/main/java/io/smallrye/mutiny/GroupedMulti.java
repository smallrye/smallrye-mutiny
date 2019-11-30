package io.smallrye.mutiny;

/**
 * A specialization of {@link Multi} used by the {@code group} operation. It allows retrieving the computed key
 * associated with this {@link Multi}
 *
 * @param <K> the type of the key
 * @param <T> the type of the items emitted by this {@link Multi}
 */
public interface GroupedMulti<K, T> extends Multi<T> {

    /**
     * @return the key associated with this {@link GroupedMulti}
     */
    K key();
}
