package io.smallrye.mutiny.operators;

import java.time.Duration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.multicast.MultiPublishOp;

public class MultiBroadcaster {

    public static <T> Multi<T> publish(Multi<T> upstream, int numberOfSubscribers, boolean cancelWhenNoOneIsListening,
            Duration delayAfterLastDeparture) {

        if (numberOfSubscribers > 0) {
            if (cancelWhenNoOneIsListening) {
                if (delayAfterLastDeparture != null) {
                    return MultiPublishOp.create(upstream)
                            .referenceCount(numberOfSubscribers, delayAfterLastDeparture);
                } else {
                    return MultiPublishOp.create(upstream).referenceCount(numberOfSubscribers, delayAfterLastDeparture);
                }
            } else {
                return MultiPublishOp.create(upstream).connectAfter(numberOfSubscribers);
            }
        } else {
            if (cancelWhenNoOneIsListening) {
                if (delayAfterLastDeparture != null) {
                    return MultiPublishOp.create(upstream).referenceCount(1, delayAfterLastDeparture);
                } else {
                    return MultiPublishOp.create(upstream).referenceCount();
                }
            } else {
                return MultiPublishOp.create(upstream).connectAfter(1);
            }
        }
    }

    private MultiBroadcaster() {
        // Avoid direct instantiation.
    }
}
