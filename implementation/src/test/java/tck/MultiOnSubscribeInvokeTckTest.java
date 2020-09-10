package tck;

import org.reactivestreams.Publisher;

public class MultiOnSubscribeInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onSubscribe().invoke(x -> {
                    // noop
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onSubscribe().invoke(x -> {
                    // noop
                });
    }
}
