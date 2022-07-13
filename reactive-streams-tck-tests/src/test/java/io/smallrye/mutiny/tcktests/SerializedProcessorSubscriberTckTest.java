package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import org.reactivestreams.tck.SubscriberWhiteboxVerification;

import io.smallrye.mutiny.subscription.SerializedSubscriber;

public class SerializedProcessorSubscriberTckTest extends AbstractWhiteBoxSubscriberTck {

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber(
            SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<Integer> probe) {
        return new SerializedSubscriber<>(createReportingDownstreamSubscriber(probe));
    }
}
