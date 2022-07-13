package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnCancellationCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onCancellation().call(() -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onCancellation().call(() -> Uni.createFrom().nullItem());
    }
}
