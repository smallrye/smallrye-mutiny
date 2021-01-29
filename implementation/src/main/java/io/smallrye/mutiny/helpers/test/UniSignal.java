package io.smallrye.mutiny.helpers.test;

/**
 * A signal: onSubscribe, onItem, onFailure or cancel.
 */
public interface UniSignal {

    /**
     * Get the signal associated value, if any.
     *
     * @return the value
     */
    Object value();
}
