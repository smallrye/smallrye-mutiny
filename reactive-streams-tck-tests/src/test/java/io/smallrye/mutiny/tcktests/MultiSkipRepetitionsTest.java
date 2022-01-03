package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiSkipRepetitionsTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .skip().repetitions();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .skip().repetitions();
    }

}
