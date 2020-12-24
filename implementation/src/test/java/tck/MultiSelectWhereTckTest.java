package tck;

import org.reactivestreams.Publisher;

public class MultiSelectWhereTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .select().where(l -> true);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .select().where(l -> true);
    }

}
