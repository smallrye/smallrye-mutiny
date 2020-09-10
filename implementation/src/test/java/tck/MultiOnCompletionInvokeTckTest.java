package tck;

import org.reactivestreams.Publisher;

public class MultiOnCompletionInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onCompletion().invoke(() -> {
                    // Do nothing
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onCompletion().invoke(() -> {
                    // noop
                });
    }
}
