package io.smallrye.mutiny;

/**
 * Exception thrown when an operation times out.
 * Unlike {@link java.util.concurrent.TimeoutException}, this variant is a {@link RuntimeException}.
 */
public class TimeoutException extends RuntimeException {

    /**
     * Constructs a {@code TimeoutException} with no specified detail message.
     */
    public TimeoutException() {
        super();
    }

}
