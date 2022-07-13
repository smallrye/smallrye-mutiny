package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Uni;

public class MultiOnItemTransformToUniAndConcatenateTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onItem().transformToUniAndConcatenate(x -> Uni.createFrom().item(x));
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onItem().transformToUniAndConcatenate(x -> Uni.createFrom().item(x));
    }
}
