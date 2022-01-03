package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnItemTransformToUniAndMergeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onItem().transformToUniAndMerge(x -> Uni.createFrom().item(x));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onItem().transformToUniAndMerge(x -> Uni.createFrom().item(x));
    }
}
