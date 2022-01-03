package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;

public class MultiOnCancellationInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onCancellation().invoke(() -> {
                    // Do nothing
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onCancellation().invoke(() -> {
                    // noop
                });
    }
}
