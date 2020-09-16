package tck;

import org.junit.jupiter.api.Disabled;
import org.reactivestreams.Publisher;

public class MultiBroadcastToAllSubscribersTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .broadcast().toAllSubscribers();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .broadcast().toAllSubscribers();
    }

    @Override
    @Disabled
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() {
        // The broadcast is capping at Long.MAX.
    }

}
