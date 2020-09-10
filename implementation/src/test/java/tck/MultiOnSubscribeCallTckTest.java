package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiOnSubscribeCallTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onSubscribe().call(x -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onSubscribe().call(x -> Uni.createFrom().nullItem());
    }
}
