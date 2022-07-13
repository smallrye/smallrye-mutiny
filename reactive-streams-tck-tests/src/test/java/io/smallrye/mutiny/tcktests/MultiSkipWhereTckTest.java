package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

public class MultiSkipWhereTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .skip().where(l -> false);
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .skip().where(l -> false);
    }

}
