package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

public class MultiDemandCappingTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements).capDemandsTo(4096L);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream().capDemandsTo(4096L);
    }
}
