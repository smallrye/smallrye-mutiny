package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import org.reactivestreams.tck.SubscriberWhiteboxVerification;

import io.smallrye.mutiny.helpers.StrictMultiSubscriber;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class StrictMultiSubscriberTckTest extends AbstractWhiteBoxSubscriberTck {

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber(
            SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<Integer> probe) {
        MultiSubscriber<? super Integer> downstream = createReportingDownstreamSubscriber(probe);
        return new StrictMultiSubscriber<>(downstream);
    }

}
