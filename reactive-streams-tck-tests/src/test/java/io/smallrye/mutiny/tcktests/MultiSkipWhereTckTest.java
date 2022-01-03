package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiSkipWhereTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .skip().where(l -> false);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .skip().where(l -> false);
    }

}
