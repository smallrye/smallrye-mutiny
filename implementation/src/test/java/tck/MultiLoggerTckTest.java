package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Uni;

public class MultiLoggerTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .log();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .log();
    }
}
