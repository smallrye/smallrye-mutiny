package io.smallrye.reactive.subscription;

import io.smallrye.reactive.Uni;

/**
 * Will receive call to {@link #onSubscribe(UniSubscription)} once after passing an instance of this {@link UniSubscriber}
 * to {@link io.smallrye.reactive.groups.UniSubscribe#withSubscriber(UniSubscriber)} retrieved from {@link Uni#subscribe()}.
 * Unlike Reactive Streams Subscriber, {@link UniSubscriber} does not request results.
 * <p>
 * After subscription, it can receive:
 * <ul>
 * <li>a <em>result</em> event triggering {@link #onResult(Object)}. The result can be {@code null}.</li>
 * <li>a <em>failure</em> event triggering {@link #onFailure(Throwable)} which signals an error state.</li>
 * </ul>
 * <p>
 * Once this subscriber receives a result or failure event, no more events will be received.
 * <p>
 * Note that unlike in Reactive Streams, the value received in {@link #onResult(Object)} can be {@code null}.
 *
 * @param <T> the expected type of result
 */
public interface UniSubscriber<T> {

    /**
     * Event handler called once the subscribed {@link Uni} has taken into account the subscription. The {@link Uni}
     * have triggered the computation of the result.
     *
     * <strong>IMPORTANT:</strong> {@link #onResult(Object)} and {@link #onFailure(Throwable)} would not be called
     * before the invocation of this method.
     *
     * <ul>
     *     <li>Executor: Operate on no particular executor, except if {@link Uni#callSubscribeOn} has been called</li>
     *     <li>Exception: Throwing an exception cancels the subscription, {@link #onResult(Object)} and
     *     {@link #onFailure(Throwable)} won't be called</li>
     * </ul>
     *
     * @param subscription the subscription allowing to cancel the computation.
     */
    void onSubscribe(UniSubscription subscription);

    /**
     * Event handler called once the result has been computed by the subscribed {@link Uni}.
     *
     * <strong>IMPORTANT:</strong> this method will be only called once per subscription. If
     * {@link #onFailure(Throwable)} is called, this method won't be called.
     *
     * <ul>
     *     <li>Executor: Operate on no particular executor, except if {@link Uni#handleResultOn} has been called</li>
     *     <li>Exception: Throwing an exception cancels the subscription.
     * </ul>
     *
     * @param result the result, may be {@code null}.
     */
    void onResult(T result);

    /**
     * Called if the computation of the result by the subscriber {@link Uni} failed.
     *
     * <strong>IMPORTANT:</strong> this method will be only called once per subscription. If
     * {@link #onResult(Object)} is called, this method won't be called.
     *
     * <ul>
     *     <li>Executor: Operate on no particular executor, except if {@link Uni#handleResultOn} has been called</li>
     *     <li>Exception: Throwing an exception cancels the subscription.
     * </ul>
     *
     * @param failure the failure, cannot be {@code null}.
     */
    void onFailure(Throwable failure);

}
