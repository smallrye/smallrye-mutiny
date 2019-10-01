package io.smallrye.reactive.subscription;

public interface Cancellable {

    /**
     * Runs the cancellation action.
     */
    void cancel();
}
