package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnFailureCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onFailure().call(x -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onFailure().call(x -> Uni.createFrom().nullItem());
    }
}
