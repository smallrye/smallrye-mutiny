package tck;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.subscription.SerializedSubscriber;

public class SerializedProcessorSubscriberTckTest extends AbstractWhiteBoxSubscriberTck {

    @Override
    public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        return new SerializedSubscriber<>(createReportingDownstreamSubscriber(probe));
    }
}
