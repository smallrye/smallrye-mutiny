package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

public class MultiLoggerTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .log();
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .log();
    }
}
