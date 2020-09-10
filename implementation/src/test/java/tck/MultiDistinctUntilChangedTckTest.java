package tck;

import org.reactivestreams.Publisher;

public class MultiDistinctUntilChangedTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .transform().byDroppingRepetitions();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .transform().byDroppingRepetitions();
    }

}
