package mutiny.zero;

import java.util.function.LongConsumer;

/**
 * A {@link Tube} is a general-purpose abstraction for creating {@link org.reactivestreams.Publisher}.
 * <p>
 * Items, errors and completion signals can be sent using this interface.
 * It is possible to be notified of requests, cancellations and termination.
 * <p>
 * A {@link Tube} can be shared between multiple threads, and sending items from concurrent threads is done serially as
 * per reactive stream semantics.
 * <p>
 * If in doubt about which abstraction to use for creating a {@link org.reactivestreams.Publisher} with
 * {@link ZeroPublisher} then choose a {@link Tube}.
 *
 * @param <T> the items type
 */
public interface Tube<T> {

    /**
     * Send an item.
     * 
     * @param item the item
     * @return this {@link Tube} instance
     */
    Tube<T> send(T item);

    /**
     * Terminally signal an error.
     * 
     * @param err the error
     */
    void fail(Throwable err);

    /**
     * Signal completion and that no more items will be sent.
     */
    void complete();

    /**
     * Check if the subscription has been cancelled.
     * 
     * @return {@code true} if the subscriber has cancelled its subscription, {@code false} otherwise
     */
    boolean cancelled();

    /**
     * Check the number of outstanding requests.
     * 
     * @return the number of outstanding requests.
     */
    long outstandingRequests();

    /**
     * Define an action when the subscription is cancelled.
     * 
     * @param action the action
     * @return this {@link Tube}
     */
    Tube<T> whenCancelled(Runnable action);

    /**
     * Define an action on termination (completion, error or cancellation), typically for cleanup purposes.
     * 
     * @param action the action
     * @return this {@link Tube}
     */
    Tube<T> whenTerminates(Runnable action);

    /**
     * Define an action when items are being requested.
     * 
     * @param consumer the action, consuming the number of items for this request
     * @return this {@link Tube}
     */
    Tube<T> whenRequested(LongConsumer consumer);

}
