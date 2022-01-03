package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;
import org.testng.annotations.Ignore;

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
    @Ignore
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() {
        // The overflow is capping at Long.MAX.
    }

    @Override
    @Ignore
    public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements()
            throws Throwable {
        // Elements may be dropped
    }

    @Override
    @Ignore
    public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
        // The overflow drops the previous items.
    }

    @Override
    @Ignore
    public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue()
            throws Throwable {
        // The overflow drops the previous items.
    }

    @Override
    @Ignore
    public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
        // The overflow drops the previous items.
    }

    @Override
    @Ignore
    public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
        // The overflow drops the previous items.
    }

    @Override
    @Ignore
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
        // The overflow drops the previous items.
    }
}
