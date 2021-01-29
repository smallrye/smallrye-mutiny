package io.smallrye.mutiny.helpers.test;

/**
 * A cancellation signal.
 */
public final class OnCancellationUniSignal implements UniSignal {

    @Override
    public Void value() {
        return null;
    }

    @Override
    public String toString() {
        return "OnCancellationSignal{}";
    }
}
