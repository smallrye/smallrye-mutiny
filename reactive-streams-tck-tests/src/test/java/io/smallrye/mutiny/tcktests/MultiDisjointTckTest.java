package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;
import java.util.stream.LongStream;

import io.smallrye.mutiny.Multi;

public class MultiDisjointTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return Multi.createFrom().item(() -> list)
                .onItem().disjoint();
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onItem().disjoint();
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
