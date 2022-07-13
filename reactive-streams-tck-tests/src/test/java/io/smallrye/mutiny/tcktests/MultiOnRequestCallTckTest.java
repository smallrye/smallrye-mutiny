package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnRequestCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onRequest().call((count) -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onRequest().call((count) -> Uni.createFrom().nullItem());
    }
}
