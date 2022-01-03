package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiSelectWhenTckTest extends AbstractPublisherTck<Long> {

    private static final Uni<Boolean> UNI_PRODUCING_TRUE = Uni.createFrom().item(true);

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .select().when(l -> UNI_PRODUCING_TRUE);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .select().when(l -> UNI_PRODUCING_TRUE);
    }

}
