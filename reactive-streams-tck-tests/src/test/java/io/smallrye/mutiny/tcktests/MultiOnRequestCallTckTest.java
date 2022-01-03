package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnRequestCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onRequest().call((count) -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onRequest().call((count) -> Uni.createFrom().nullItem());
    }
}
