package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiSkipFirstItemsTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .skip().first(0);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .skip().first(0);
    }
}
