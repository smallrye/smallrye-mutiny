package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiOnRequestInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onRequest().invoke((count) -> {
                    // Do nothing
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onRequest().invoke((count) -> {
                    // noop
                });
    }

}
