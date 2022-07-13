package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

public class MultiSelectLastTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .select().last((int) elements);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .select().last();
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
