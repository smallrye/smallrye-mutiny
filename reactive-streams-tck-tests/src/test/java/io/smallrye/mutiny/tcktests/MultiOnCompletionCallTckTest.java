package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Uni;

public class MultiOnCompletionCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onCompletion().call(() -> Uni.createFrom().nullItem());
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onCompletion().call(() -> Uni.createFrom().nullItem());
    }
}
