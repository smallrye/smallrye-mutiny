package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnCancellationCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onCancellation().call(() -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onCancellation().call(() -> Uni.createFrom().nullItem());
    }
}
