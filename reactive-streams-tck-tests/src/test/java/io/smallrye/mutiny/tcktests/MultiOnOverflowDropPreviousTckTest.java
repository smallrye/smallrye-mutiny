package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import org.testng.annotations.Ignore;

public class MultiOnOverflowDropPreviousTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .onOverflow().dropPreviousItems();
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
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
