package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Uni;

public class MultiOnItemCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onItem().call(x -> Uni.createFrom().nullItem());
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onItem().call(x -> Uni.createFrom().nullItem());
    }
}
