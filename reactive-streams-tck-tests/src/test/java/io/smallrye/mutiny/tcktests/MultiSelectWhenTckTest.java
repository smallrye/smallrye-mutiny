package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Uni;

public class MultiSelectWhenTckTest extends AbstractPublisherTck<Long> {

    private static final Uni<Boolean> UNI_PRODUCING_TRUE = Uni.createFrom().item(true);

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .select().when(l -> UNI_PRODUCING_TRUE);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .select().when(l -> UNI_PRODUCING_TRUE);
    }

}
