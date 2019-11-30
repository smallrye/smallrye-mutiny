package io.smallrye.mutiny.subscription;

public interface Cancellable {

    /**
     * Runs the cancellation action.
     */
    void cancel();

}
