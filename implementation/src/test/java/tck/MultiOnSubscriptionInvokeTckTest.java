package tck;

import org.reactivestreams.Publisher;

public class MultiOnSubscriptionInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onSubscription().invoke(x -> {
                    // noop
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onSubscription().invoke(x -> {
                    // noop
                });
    }
}
