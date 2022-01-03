package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiOnItemInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onItem().invoke(x -> {
                    // noop
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onItem().invoke(x -> {
                    // noop
                });
    }
}
