package mutiny.zero;

/**
 * Define a {@link Tube} back-pressure management strategy.
 * 
 * A {@link Tube} back-pressure management is required when an item is sent to a {@link Tube} while there are no
 * outstanding items being requested.
 */
public enum BackpressureStrategy {

    /**
     * Buffer overflowing items until more items are being requested.
     * The buffer is bounded, and an {@link IllegalStateException} will be thrown if it becomes full due to a lack of
     * requests.
     */
    BUFFER,

    /**
     * Buffer overflowing items until more items are being requested.
     * The buffer is unbounded, so available memory is the limit, meaning that an {@link OutOfMemoryError} is possible
     * if items are being pushed faster than they are being consumed.
     */
    UNBOUNDED_BUFFER,

    /**
     * Drop items in case of a lack of outstanding requests.
     */
    DROP,

    /**
     * Signal a terminal {@link IllegalStateException} as soon as an item is being sent while there is no outstanding
     * request.
     */
    ERROR,

    /**
     * Ignore back-pressure and still send items to the {@link Tube} consumer.
     * This may result in errors in the subscriber(s) depending on what it means for back-pressure to be ignored.
     */
    IGNORE,

    /**
     * Buffer overflowing items in a bounded buffer, but only keep the last values.
     * This means that if the overflow buffer becomes full then the oldest item is discarded to make space for a new one.
     */
    LATEST
}
