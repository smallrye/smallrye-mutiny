package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiDemandCappingTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements).capDemandsTo(4096L);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream().capDemandsTo(4096L);
    }
}
