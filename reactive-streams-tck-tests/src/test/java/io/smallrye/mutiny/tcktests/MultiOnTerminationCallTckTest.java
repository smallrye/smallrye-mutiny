package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Uni;

public class MultiOnTerminationCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onTermination().call((t, c) -> Uni.createFrom().nullItem());
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onTermination().call((t, c) -> Uni.createFrom().nullItem());
    }
}
