package io.smallrye.reactive.unimulti.subscription;

public interface Cancellable {

    /**
     * Runs the cancellation action.
     */
    void cancel();
}
