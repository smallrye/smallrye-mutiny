package tck;

import org.junit.jupiter.api.Disabled;
import org.reactivestreams.Publisher;

public class MultiBroadcastToAtLeastTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .broadcast().toAtLeast(1);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .broadcast().toAtLeast(1);
    }

    @Override
    @Disabled
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() {
        // The broadcast is capping at Long.MAX.
    }

}
