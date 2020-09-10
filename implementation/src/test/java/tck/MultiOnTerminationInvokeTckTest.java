package tck;

import org.reactivestreams.Publisher;

public class MultiOnTerminationInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onTermination().invoke(() -> {
                    // Do nothing
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onTermination().invoke(() -> {
                    // noop
                });
    }
}
