package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnItemTransformToUniAndConcatenateTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onItem().transformToUniAndConcatenate(x -> Uni.createFrom().item(x));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onItem().transformToUniAndConcatenate(x -> Uni.createFrom().item(x));
    }
}
