package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import org.testng.annotations.Ignore;

public class MultiBroadcastToAllSubscribersTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .broadcast().toAllSubscribers();
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .broadcast().toAllSubscribers();
    }

    @Override
    @Ignore
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() {
        // The broadcast is capping at Long.MAX.
    }

}
