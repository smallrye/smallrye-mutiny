package tck;

import org.junit.jupiter.api.Disabled;
import org.reactivestreams.Publisher;

public class MultiOnOverflowDropTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onOverflow().drop();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onOverflow().drop();
    }

    @Override
    @Disabled
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() {
        // The overflow is capping at Long.MAX.
    }

    @Override
    @Disabled
    public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements()
            throws Throwable {
        // The overflow will drop if not consumed
    }

    @Override
    @Disabled
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() {
        // The overflow will drop if not consumed
    }

    @Override
    @Disabled
    public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
        // The overflow will drop if not consumed
    }
}
