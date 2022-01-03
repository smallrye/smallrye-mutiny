package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiSelectLastTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .select().last((int) elements);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .select().last();
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
