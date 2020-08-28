/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/

package org.reactivestreams.tck.junit5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Provides tests for verifying {@code Publisher} specification rules.
 *
 * @see org.reactivestreams.Publisher
 */
public abstract class PublisherVerification<T> extends org.reactivestreams.tck.PublisherVerification<T> {

    public PublisherVerification(org.reactivestreams.tck.TestEnvironment env) {
        super(env);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    @Override
    public void required_createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable {
        super.required_createPublisher1MustProduceAStreamOfExactly1Element();
    }

    @Test
    @Override
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
        super.required_createPublisher3MustProduceAStreamOfExactly3Elements();
    }

    @Test
    @Override
    public void required_validate_maxElementsFromPublisher() throws Exception {
        super.required_validate_maxElementsFromPublisher();
    }

    @Test
    @Override
    public void required_validate_boundedDepthOfOnNextAndRequestRecursion() throws Exception {
        super.required_validate_boundedDepthOfOnNextAndRequestRecursion();
    }

    @Test
    @Override
    public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements()
            throws Throwable {
        super.required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements();
    }

    @Test
    @Override
    public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
        super.required_spec102_maySignalLessThanRequestedAndTerminateSubscription();
    }

    @Test
    @Override
    public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
        super.stochastic_spec103_mustSignalOnMethodsSequentially();
    }

    @Test
    @Override
    public void optional_spec104_mustSignalOnErrorWhenFails() throws Throwable {
        super.optional_spec104_mustSignalOnErrorWhenFails();
    }

    @Test
    @Override
    public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
        super.required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates();
    }

    @Test
    @Override
    public void optional_spec105_emptyStreamMustTerminateBySignallingOnComplete() throws Throwable {
        super.optional_spec105_emptyStreamMustTerminateBySignallingOnComplete();
    }

    @Test
    @Override
    public void untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled()
            throws Throwable {
        // Untested - do nothing
    }

    @Test
    @Override
    public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() throws Throwable {
        super.required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled();
    }

    @Test
    @Override
    public void untested_spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() {
        // Untested - do nothing
    }

    @Test
    @Override
    public void untested_spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() {
        // Do nothing - untested
    }

    @Test
    @Override
    public void untested_spec109_subscribeShouldNotThrowNonFatalThrowable() {
        // Do nothing - untested
    }

    @Test
    @Override
    public void required_spec109_subscribeThrowNPEOnNullSubscriber() throws Throwable {
        super.required_spec109_subscribeThrowNPEOnNullSubscriber();
    }

    @Test
    @Override
    public void required_spec109_mustIssueOnSubscribeForNonNullSubscriber() throws Throwable {
        super.required_spec109_mustIssueOnSubscribeForNonNullSubscriber();
    }

    @Test
    @Override
    public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe()
            throws Throwable {
        super.required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe();
    }

    @Test
    @Override
    public void untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice()
            throws Throwable {
        // Do nothing - untested
    }

    @Test
    @Override
    public void optional_spec111_maySupportMultiSubscribe() throws Throwable {
        super.optional_spec111_maySupportMultiSubscribe();
    }

    @Test
    @Override
    public void optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals()
            throws Throwable {
        super.optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals();
    }

    @Test
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne()
            throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne();
    }

    @Test
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront()
            throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront();
    }

    @Test
    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected()
            throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected();
    }

    @Test
    @Override
    public void required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable {
        super.required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe();
    }

    @Test
    @Override
    public void required_spec303_mustNotAllowUnboundedRecursion() throws Throwable {
        super.required_spec303_mustNotAllowUnboundedRecursion();
    }

    @Test
    @Override
    public void untested_spec304_requestShouldNotPerformHeavyComputations() throws Exception {
        // Do nothing - untested
    }

    @Test
    @Override
    public void untested_spec305_cancelMustNotSynchronouslyPerformHeavyComputation() throws Exception {
        // Do nothing - untested
    }

    @Test
    @Override
    public void required_spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable {
        super.required_spec306_afterSubscriptionIsCancelledRequestMustBeNops();
    }

    @Test
    @Override
    public void required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops()
            throws Throwable {
        super.required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops();
    }

    @Test
    @Override
    public void required_spec309_requestZeroMustSignalIllegalArgumentException() throws Throwable {
        super.required_spec309_requestZeroMustSignalIllegalArgumentException();
    }

    @Test
    @Override
    public void required_spec309_requestNegativeNumberMustSignalIllegalArgumentException() throws Throwable {
        super.required_spec309_requestNegativeNumberMustSignalIllegalArgumentException();
    }

    @Test
    @Override
    public void optional_spec309_requestNegativeNumberMaySignalIllegalArgumentExceptionWithSpecificMessage()
            throws Throwable {
        super.optional_spec309_requestNegativeNumberMaySignalIllegalArgumentExceptionWithSpecificMessage();
    }

    @Test
    @Override
    public void required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling() throws Throwable {
        super.required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling();
    }

    @Test
    @Override
    public void required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber()
            throws Throwable {
        super.required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber();
    }

    @Test
    @Override
    public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
        super.required_spec317_mustSupportAPendingElementCountUpToLongMaxValue();
    }

    @Test
    @Override
    public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue()
            throws Throwable {
        super.required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue();
    }

    @Test
    @Override
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
        if (maxElementsFromPublisher() < Long.MAX_VALUE - 1) {
            System.err.println("[Not Verified] Unable to run this test, as required elements nr: 2147483647 is higher "
                    + "than supported by given producer: " + maxElementsFromPublisher());
            return;
        }
        super.required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue();
    }

    @Override
    public void notVerified() {
        System.err.println("Not verified by this TCK");
    }

    @Override
    public void notVerified(String message) {
        System.err.println("[Not Verified] " + message);
    }

}