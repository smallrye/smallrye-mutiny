package io.smallrye.mutiny.subscription;

import java.util.concurrent.Flow.Subscription;

import io.smallrye.mutiny.Uni;

/**
 * A {@link Subscription} for the {@link Uni} type.
 * <p>
 * The main different with the Reactive Streams Subscription is about the <em>request</em> protocol. Uni does not use
 * request and triggers the computation at subscription time.
 */
public interface UniSubscription extends Subscription, Cancellable {

    /**
     * Requests the {@link Uni} to cancel and clean up resources.
     * If the item is retrieved after cancellation, it is not forwarded to the subscriber.
     * If the cancellation happens after the delivery of the item, this call is ignored.
     * <p>
     * Calling this method, emits the {@code cancellation} event upstream.
     */
    void cancel();

    @Override
    default void request(long n) {
        if (n < 1) {
            throw new IllegalArgumentException("Invalid request");
        }
        // Ignored, on Uni the request happen at subscription time.
    }
}
