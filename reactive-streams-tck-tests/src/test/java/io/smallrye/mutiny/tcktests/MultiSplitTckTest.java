package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

public class MultiSplitTckTest extends AbstractPublisherTck<Long> {

    enum Anything {
        AnyValue
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .split(Anything.class, n -> Anything.AnyValue)
                .get(Anything.AnyValue);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .split(Anything.class, n -> Anything.AnyValue)
                .get(Anything.AnyValue);
    }
}
