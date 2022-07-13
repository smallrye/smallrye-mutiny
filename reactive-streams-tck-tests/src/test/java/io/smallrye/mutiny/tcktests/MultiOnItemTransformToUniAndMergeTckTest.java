package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnItemTransformToUniAndMergeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onItem().transformToUniAndMerge(x -> Uni.createFrom().item(x));
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onItem().transformToUniAndMerge(x -> Uni.createFrom().item(x));
    }
}
