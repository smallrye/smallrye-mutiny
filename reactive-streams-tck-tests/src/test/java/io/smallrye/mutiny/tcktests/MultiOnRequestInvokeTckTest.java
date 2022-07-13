package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

public class MultiOnRequestInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onRequest().invoke((count) -> {
                    // Do nothing
                });
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onRequest().invoke((count) -> {
                    // noop
                });
    }

}
