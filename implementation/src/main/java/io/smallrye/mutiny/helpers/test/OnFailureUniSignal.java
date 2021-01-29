package io.smallrye.mutiny.helpers.test;

/**
 * A onFailure signal.
 */
public final class OnFailureUniSignal implements UniSignal {
    private final Throwable failure;

    public OnFailureUniSignal(Throwable failure) {
        this.failure = failure;
    }

    @Override
    public Throwable value() {
        return failure;
    }

    @Override
    public String toString() {
        return "OnFailureSignal{" +
                "failure=" + failure +
                '}';
    }
}
