package io.smallrye.reactive.operators;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

public class MultiBroadcaster {

    static <T> Flowable<T> getFlowable(Multi<T> upstream) {
        return Flowable.fromPublisher(upstream);
    }

    public static <T> Multi<T> publish(Multi<T> upstream, int numberOfSubscribers, boolean cancelWhenNoOneIsListening,
            Duration delayAfterLastDeparture) {

        Flowable<T> flowable = getFlowable(upstream);

        if (numberOfSubscribers > 0) {
            if (cancelWhenNoOneIsListening) {
                if (delayAfterLastDeparture != null) {
                    return new DefaultMulti<>(flowable.publish().refCount(numberOfSubscribers,
                            delayAfterLastDeparture.toMillis(), TimeUnit.MILLISECONDS));
                } else {
                    return new DefaultMulti<>(flowable.publish().refCount(numberOfSubscribers));
                }
            } else {
                return new DefaultMulti<>(flowable.publish().autoConnect(numberOfSubscribers));
            }
        } else {
            if (cancelWhenNoOneIsListening) {
                if (delayAfterLastDeparture != null) {
                    return new DefaultMulti<>(
                            flowable.publish().refCount(delayAfterLastDeparture.toMillis(), TimeUnit.MILLISECONDS));
                } else {
                    return new DefaultMulti<>(flowable.publish().refCount());
                }
            } else {
                return new DefaultMulti<>(flowable.publish().autoConnect());
            }
        }
    }

    private MultiBroadcaster() {
        // Avoid direct instantiation.
    }
}
