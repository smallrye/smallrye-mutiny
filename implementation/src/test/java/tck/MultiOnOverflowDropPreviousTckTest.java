package tck;

import org.junit.jupiter.api.Disabled;
import org.reactivestreams.Publisher;

public class MultiOnOverflowDropPreviousTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onOverflow().dropPreviousItems();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onOverflow().dropPreviousItems();
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
        // Elements may be dropped
    }

    @Override
    @Disabled
    public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
        // The overflow drops the previous items.
    }

    @Override
    @Disabled
    public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue()
            throws Throwable {
        // The overflow drops the previous items.
    }

    @Override
    @Disabled
    public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
        // The overflow drops the previous items.
    }

    @Override
    @Disabled
    public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
        // The overflow drops the previous items.
    }

    @Override
    @Disabled
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
        // The overflow drops the previous items.
    }
}
