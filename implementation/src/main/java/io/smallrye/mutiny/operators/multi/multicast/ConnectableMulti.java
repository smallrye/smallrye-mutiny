
package io.smallrye.mutiny.operators.multi.multicast;

import java.time.Duration;
import java.util.concurrent.Flow;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;

/**
 * A {@code ConnectableMulti} is similar to a regular {@link Multi}, except that it does not begin
 * emitting items (from upstream to downstream) when it is subscribed to, but only when its {@link #connect} method is
 * called. This allows deferring the dispatching of events. For example, it can wait until a set number of subscribers
 * have subscribed.
 *
 * @param <T> the type of item
 */
public abstract class ConnectableMulti<T> extends AbstractMulti<T> {

    protected final Multi<T> upstream;

    protected ConnectableMulti(Multi<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * Allows this {@link ConnectableMulti} to start emitting the items from its upstream {@link Multi} to
     * its {@link Flow.Subscriber}s.
     *
     * @param connection the connection.
     */
    protected abstract void connect(ConnectableMultiConnection connection);

    /**
     * Returns a {@code Multi} that stays connected to this {@code ConnectableMulti} as long as there
     * is at least one active subscription.
     *
     * @return a {@link Multi}
     */
    public Multi<T> referenceCount() {
        return Infrastructure.onMultiCreation(new MultiReferenceCount<>(this));
    }

    /**
     * Connects to the upstream {@code ConnectableMulti} if the number of subscribers reaches the specified amount and
     * disconnect after the specified duration after all subscribers have unsubscribed (cancelled their subscriptions).
     *
     * @param count the number of subscribers that trigger the emissions
     * @param duration the duration, can be {@code null}, if set must be positive
     * @return the new Multi instance
     */
    public Multi<T> referenceCount(int count, Duration duration) {
        if (duration != null) {
            ParameterValidation.validate(duration, "duration");
        }
        ParameterValidation.positive(count, "count");
        return Infrastructure.onMultiCreation(new MultiReferenceCount<>(this, count, duration));
    }

    /**
     * Returns a {@link Multi} that connect to the upstream as soon as the {@code numberOfSubscribers} subscribers
     * subscribe.
     * The connection stays opens even if the subscribers cancelled the subscription.
     * Other subscribers can subscribe, it would not re-subscribe to the upstream.
     *
     * @param numberOfSubscribers the number of subscribe to reach before subscribing to upstream.
     * @return the multi
     */
    public Multi<T> connectAfter(int numberOfSubscribers) {
        ParameterValidation.positive(numberOfSubscribers, "numberOfSubscribers");
        return Infrastructure.onMultiCreation(new MultiConnectAfter<>(this, numberOfSubscribers, null));
    }

}
