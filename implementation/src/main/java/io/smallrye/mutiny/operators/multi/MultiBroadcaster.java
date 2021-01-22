package io.smallrye.mutiny.operators.multi;

import java.time.Duration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.multicast.MultiPublishOp;

public class MultiBroadcaster {

    public static <T> Multi<T> publish(Multi<T> upstream, int numberOfSubscribers, boolean cancelWhenNoOneIsListening,
            Duration delayAfterLastDeparture) {

        if (numberOfSubscribers > 0) {
            return createPublishWithSubscribersThreshold(upstream, numberOfSubscribers, cancelWhenNoOneIsListening,
                    delayAfterLastDeparture);
        } else {
            return createPublishImmediate(upstream, cancelWhenNoOneIsListening, delayAfterLastDeparture);
        }
    }

    private static <T> Multi<T> createPublishImmediate(Multi<T> upstream, boolean cancelWhenNoOneIsListening,
            Duration delayAfterLastDeparture) {
        if (cancelWhenNoOneIsListening) {
            if (delayAfterLastDeparture != null) {
                return Infrastructure
                        .onMultiCreation(MultiPublishOp.create(upstream).referenceCount(1, delayAfterLastDeparture));
            } else {
                return Infrastructure.onMultiCreation(MultiPublishOp.create(upstream).referenceCount());
            }
        } else {
            return Infrastructure.onMultiCreation(MultiPublishOp.create(upstream).connectAfter(1));
        }
    }

    private static <T> Multi<T> createPublishWithSubscribersThreshold(Multi<T> upstream, int numberOfSubscribers,
            boolean cancelWhenNoOneIsListening, Duration delayAfterLastDeparture) {
        if (cancelWhenNoOneIsListening) {
            if (delayAfterLastDeparture != null) {
                return Infrastructure.onMultiCreation(
                        MultiPublishOp.create(upstream).referenceCount(numberOfSubscribers, delayAfterLastDeparture));
            } else {
                // the duration can be `null`, it will be validated if not `null`.
                return Infrastructure
                        .onMultiCreation(MultiPublishOp.create(upstream).referenceCount(numberOfSubscribers, null));
            }
        } else {
            return Infrastructure.onMultiCreation(MultiPublishOp.create(upstream).connectAfter(numberOfSubscribers));
        }
    }

    private MultiBroadcaster() {
        // Avoid direct instantiation.
    }
}
