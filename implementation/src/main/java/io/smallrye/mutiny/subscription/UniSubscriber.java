package io.smallrye.mutiny.subscription;

import java.util.concurrent.Executor;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniSubscribe;

/**
 * Will receive call to {@link #onSubscribe(UniSubscription)} once after passing an instance of this {@link UniSubscriber}
 * to {@link UniSubscribe#withSubscriber(UniSubscriber)} retrieved from {@link Uni#subscribe()}.
 * Unlike Reactive Streams Subscriber, {@link UniSubscriber} does not request items.
 * <p>
 * After subscription, it can receive:
 * <ul>
 * <li>a <em>item</em> event triggering {@link #onItem(Object)}. The item can be {@code null}.</li>
 * <li>a <em>failure</em> event triggering {@link #onFailure(Throwable)} which signals an error state.</li>
 * </ul>
 * <p>
 * Once this subscriber receives an item or failure event, no more events will be received.
 * <p>
 * Note that unlike in Reactive Streams, the value received in {@link #onItem(Object)} can be {@code null}.
 *
 * @param <T> the expected type of item
 */
public interface UniSubscriber<T> extends ContextSupport {

    /**
     * Event handler called once the subscribed {@link Uni} has taken into account the subscription. The {@link Uni}
     * have triggered the computation of the item.
     * <p>
     * <strong>IMPORTANT:</strong> {@link #onItem(Object)} and {@link #onFailure(Throwable)} would not be called
     * before the invocation of this method.
     *
     * <ul>
     * <li>Executor: Operate on no particular executor, except if {@link Uni#runSubscriptionOn(Executor)} has been
     * called</li>
     * <li>Exception: Throwing an exception cancels the subscription, {@link #onItem(Object)} and
     * {@link #onFailure(Throwable)} won't be called</li>
     * </ul>
     *
     * @param subscription the subscription allowing to cancel the computation.
     */
    void onSubscribe(UniSubscription subscription);

    /**
     * Event handler called once the item has been computed by the subscribed {@link Uni}.
     * <p>
     * <strong>IMPORTANT:</strong> this method will be only called once per subscription. If
     * {@link #onFailure(Throwable)} is called, this method won't be called.
     *
     * <ul>
     * <li>Executor: Operate on no particular executor, except if {@link Uni#emitOn} has been called</li>
     * <li>Exception: Throwing an exception cancels the subscription.
     * </ul>
     *
     * @param item the item, may be {@code null}.
     */
    void onItem(T item);

    /**
     * Called if the computation of the item by the subscriber {@link Uni} failed.
     * <p>
     * <strong>IMPORTANT:</strong> this method will be only called once per subscription. If
     * {@link #onItem(Object)} is called, this method won't be called.
     *
     * <ul>
     * <li>Executor: Operate on no particular executor, except if {@link Uni#emitOn} has been called</li>
     * <li>Exception: Throwing an exception cancels the subscription.
     * </ul>
     *
     * @param failure the failure, cannot be {@code null}.
     */
    void onFailure(Throwable failure);

}
