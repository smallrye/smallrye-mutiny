package io.smallrye.mutiny.subscription;

/**
 * Interface for controlling a pausable Multi stream.
 * <p>
 * This interface defines the contract for pausing and resuming demand propagation in a reactive stream.
 * Implementations of this interface are typically bound to a {@link DemandPauser} to provide external control.
 * <p>
 * This is an internal interface used by the demand pausing operator.
 * Users should interact with {@link DemandPauser} instead.
 *
 * @see DemandPauser
 */
public interface PausableMulti {

    /**
     * Pauses demand propagation to the upstream.
     */
    void pause();

    /**
     * Resumes demand propagation to the upstream.
     */
    void resume();

    /**
     * Checks if demand propagation is currently paused.
     */
    boolean isPaused();

    /**
     * Returns the current number of buffered items.
     * Only applicable when using {@link BackPressureStrategy#BUFFER}.
     *
     * @return the number of buffered items, or 0 if no buffer is used
     */
    int bufferSize();

    /**
     * Clears the buffer if currently paused.
     * Only applicable when using {@link BackPressureStrategy#BUFFER}.
     *
     * @return {@code true} if the buffer was cleared, {@code false} if not paused or no buffer exists
     */
    boolean clearBuffer();
}
