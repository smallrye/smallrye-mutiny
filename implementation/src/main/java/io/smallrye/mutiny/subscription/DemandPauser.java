package io.smallrye.mutiny.subscription;

import io.smallrye.common.annotation.Experimental;

/**
 * A handle to control a pausable stream without holding a direct reference to the stream itself.
 * <p>
 * This handle allows pausing, resuming, and inspecting the state of a pausable stream from anywhere
 * in the application, even after the stream has been transformed or subscribed to.
 * <p>
 * Example usage:
 *
 * <pre>
 * {@code
 * DemandPauser pauser = new DemandPauser();
 *
 * Multi.createFrom().range(0, 100)
 *         .pauseDemand().using(pauser)
 *         .onItem().call(i -> Uni.createFrom().nullItem()
 *                 .onItem().delayIt().by(Duration.ofMillis(10)))
 *         .onItem().transform(i -> i * 2)
 *         .subscribe().with(System.out::println);
 *
 * // Control from anywhere
 * pauser.pause();
 * pauser.resume();
 * System.out.println("Paused: " + pauser.isPaused());
 * }
 * </pre>
 */
@Experimental("This API is still being designed and may change in the future")
public class DemandPauser {

    volatile PausableMulti multi;

    /**
     * Binds this handle to a pausable channel.
     * This is typically called internally when creating a pausable stream.
     *
     * @param multi the pausable channel to bind to
     */
    public void bind(PausableMulti multi) {
        this.multi = multi;
    }

    /**
     * Pauses the stream. Already requested items will be handled according to the configured buffer strategy.
     *
     * @throws IllegalStateException if the handle is not bound to a channel
     */
    public void pause() {
        ensureBound();
        multi.pause();
    }

    /**
     * Resumes the stream. Buffered items (if any) will be delivered before new items are requested.
     *
     * @throws IllegalStateException if the handle is not bound to a channel
     */
    public void resume() {
        ensureBound();
        multi.resume();
    }

    /**
     * Checks if the stream is currently paused.
     *
     * @return {@code true} if paused, {@code false} otherwise
     * @throws IllegalStateException if the handle is not bound to a channel
     */
    public boolean isPaused() {
        ensureBound();
        return multi.isPaused();
    }

    /**
     * Returns the current buffer size (number of items in the buffer).
     * Only applicable when using BUFFER strategy.
     *
     * @return the number of buffered items
     * @throws IllegalStateException if the handle is not bound to a channel
     */
    public int bufferSize() {
        ensureBound();
        return multi.bufferSize();
    }

    /**
     * Clears the buffer if the stream is currently paused.
     * Only applicable when using BUFFER strategy.
     *
     * @return {@code true} if the buffer was cleared, {@code false} if not paused or no buffer
     * @throws IllegalStateException if the handle is not bound to a channel
     */
    public boolean clearBuffer() {
        ensureBound();
        return multi.clearBuffer();
    }

    /**
     * Checks if this handle is bound to a channel.
     *
     * @return {@code true} if bound, {@code false} otherwise
     */
    public boolean isBound() {
        return multi != null;
    }

    private void ensureBound() {
        if (multi == null) {
            throw new IllegalStateException("DemandPauser is not bound to a stream. " +
                    "Make sure to use .pauseDemand().using(pauser) in the pausable configuration.");
        }
    }
}
