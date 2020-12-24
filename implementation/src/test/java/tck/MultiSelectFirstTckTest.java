package tck;

import org.reactivestreams.Publisher;

public class MultiSelectFirstTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .select().first(elements);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .select().first();
    }
}
