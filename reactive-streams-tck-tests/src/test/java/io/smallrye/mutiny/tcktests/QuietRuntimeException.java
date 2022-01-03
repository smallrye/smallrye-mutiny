package io.smallrye.mutiny.tcktests;

/**
 * RuntimeException with no stack trace for expected failures, to make logging not so noisy.
 */
public class QuietRuntimeException extends RuntimeException {
    public QuietRuntimeException() {
        this(null, null);
    }

    public QuietRuntimeException(String message) {
        this(message, null);
    }

    public QuietRuntimeException(String message, Throwable cause) {
        super(message, cause, true, false);
    }

    public QuietRuntimeException(Throwable cause) {
        this(null, cause);
    }
}