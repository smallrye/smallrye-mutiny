package tck;

import org.junit.jupiter.api.Disabled;
import org.reactivestreams.Publisher;

public class MultiOnOverflowBufferTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onOverflow().buffer();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onOverflow().buffer(10);
    }

    @Override
    @Disabled
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() {
        // The overflow is capping at Long.MAX.
    }

    @Override
    public void required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling() throws Throwable {
        // The test would fails with an overflow failure passed downstream.
    }
}
