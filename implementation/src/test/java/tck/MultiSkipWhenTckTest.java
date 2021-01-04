package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiSkipWhenTckTest extends AbstractPublisherTck<Long> {

    private static final Uni<Boolean> UNI_PRODUCING_FALSE = Uni.createFrom().item(false);

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .skip().when(l -> UNI_PRODUCING_FALSE);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .skip().when(l -> UNI_PRODUCING_FALSE);
    }

}
