package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import org.testng.annotations.Ignore;

public class MultiBroadcastToAtLeastTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .broadcast().toAtLeast(1);
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .broadcast().toAtLeast(1);
    }

    @Override
    @Ignore
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() {
        // The broadcast is capping at Long.MAX.
    }

}
