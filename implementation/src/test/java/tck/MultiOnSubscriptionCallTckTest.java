package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnSubscriptionCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onSubscription().call(x -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onSubscription().call(x -> Uni.createFrom().nullItem());
    }
}
