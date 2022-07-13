package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

public class MultiOnSubscriptionInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onSubscription().invoke(x -> {
                    // noop
                });
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onSubscription().invoke(x -> {
                    // noop
                });
    }
}
