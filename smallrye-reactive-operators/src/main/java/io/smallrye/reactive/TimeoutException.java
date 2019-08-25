package io.smallrye.reactive;

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

    /**
     * Constructs a {@code TimeoutException} with the specified detail message.
     *
     * @param message the detail message
     */
    public TimeoutException(String message) {
        super(message);
    }

}
