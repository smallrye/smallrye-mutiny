package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnSubscriptionCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onSubscription().call(x -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onSubscription().call(x -> Uni.createFrom().nullItem());
    }
}
