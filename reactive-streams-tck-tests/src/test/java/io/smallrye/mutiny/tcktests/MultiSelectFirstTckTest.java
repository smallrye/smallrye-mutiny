package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

public class MultiSelectFirstTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .select().first(elements);
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .select().first();
    }
}
