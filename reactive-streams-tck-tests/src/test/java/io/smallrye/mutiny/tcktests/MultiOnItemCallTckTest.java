package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnItemCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onItem().call(x -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onItem().call(x -> Uni.createFrom().nullItem());
    }
}
