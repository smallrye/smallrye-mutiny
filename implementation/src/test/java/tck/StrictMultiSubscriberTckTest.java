package tck;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.helpers.StrictMultiSubscriber;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class StrictMultiSubscriberTckTest extends AbstractWhiteBoxSubscriberTck {

    @Override
    public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        MultiSubscriber<? super Integer> downstream = createReportingDownstreamSubscriber(probe);
        return new StrictMultiSubscriber<>(downstream);
    }

}
