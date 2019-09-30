package io.smallrye.reactive.unimulti.groups;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.positive;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.validate;

import java.time.Duration;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.operators.MultiBroadcaster;

/**
 * Makes the upstream {@link Multi} be able to broadcast its events ({@code items}, {@code failure}, and
 * {@code completion}) to multiple subscribers.
 *
 * @param <T> the type of item
 */
public class MultiBroadcast<T> {

    private final Multi<T> upstream;
    private boolean cancelWhenNoOneIsListening;
    private Duration delayAfterLastDeparture;

    public MultiBroadcast(Multi<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * Broadcasts the events of the upstream {@code Multi} to all the subscribers.
     * Subscribers start receiving the events as soon as they subscribe.
     *
     * @return the {@link Multi} accepting several subscribers
     */
    public Multi<T> toAllSubscribers() {
        return MultiBroadcaster.publish(upstream, 0, cancelWhenNoOneIsListening, delayAfterLastDeparture);
    }

    /**
     * Broadcasts the events of the upstream {@code Multi} to several subscribers.
     * Subscribers start receiving the events when at least {@code numberOfSubscribers} subscribes to the produced
     * {@code Multi}.
     *
     * @param numberOfSubscribers the number of subscriber requires before subscribing to the upstream multi and start
     *        dispatching the events. Must be strictly positive.
     * @return the {@link Multi} accepting several subscribers
     */
    public Multi<T> toAtLeast(int numberOfSubscribers) {
        positive(numberOfSubscribers, "numberOfSubscribers");
        return MultiBroadcaster
                .publish(upstream, numberOfSubscribers, cancelWhenNoOneIsListening, delayAfterLastDeparture);
    }

    /**
     * Indicates that the subscription to the upstream {@code Multi} is cancelled once all the subscribers have
     * cancelled their subscription.
     *
     * @return this {@link MultiBroadcast}.
     */
    public MultiBroadcast<T> withCancellationAfterLastSubscriberDeparture() {
        cancelWhenNoOneIsListening = true;
        return this;
    }

    /**
     * Indicates that the subscription to the upstream {@code Multi} is cancelled once all the subscribers have
     * cancelled their subscription. Before cancelling it wait for a grace period of {@code delay}. If any subscriber
     * subscribes during this period, the cancellation will not happen.
     *
     * @param delay the delay, must not be {@code null}, must be positive
     * @return this {@link MultiBroadcast}.
     */
    public MultiBroadcast<T> withCancellationAfterLastSubscriberDeparture(Duration delay) {
        this.delayAfterLastDeparture = validate(delay, "delay");
        withCancellationAfterLastSubscriberDeparture();
        return this;

    }
}
