package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiOnFailureInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onFailure().invoke(x -> {
                    // noop
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onFailure().invoke(x -> {
                    // noop
                });
    }
}
