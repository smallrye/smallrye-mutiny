package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

public class MultiSkipRepetitionsTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .skip().repetitions();
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .skip().repetitions();
    }

}
