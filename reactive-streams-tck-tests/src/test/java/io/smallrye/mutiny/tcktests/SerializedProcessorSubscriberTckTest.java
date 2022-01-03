package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;

import io.smallrye.mutiny.subscription.SerializedSubscriber;

public class SerializedProcessorSubscriberTckTest extends AbstractWhiteBoxSubscriberTck {

    @Override
    public Subscriber<Integer> createSubscriber(SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<Integer> probe) {
        return new SerializedSubscriber<>(createReportingDownstreamSubscriber(probe));
    }
}
