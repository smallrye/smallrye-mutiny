package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiSkipWhenTckTest extends AbstractPublisherTck<Long> {

    private static final Uni<Boolean> UNI_PRODUCING_FALSE = Uni.createFrom().item(false);

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .skip().when(l -> UNI_PRODUCING_FALSE);
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .skip().when(l -> UNI_PRODUCING_FALSE);
    }

}
