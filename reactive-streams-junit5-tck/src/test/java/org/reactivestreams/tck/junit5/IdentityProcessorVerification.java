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
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactivestreams.tck.TestEnvironment;

@ExtendWith(TestngSkippingExtension.class)
public abstract class IdentityProcessorVerification<T>
        extends org.reactivestreams.tck.IdentityProcessorVerification<T> {

    public IdentityProcessorVerification(TestEnvironment env) {
        super(env);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @Test
    public void required_validate_maxElementsFromPublisher() throws Exception {
        super.required_validate_maxElementsFromPublisher();
    }

    @Override
    @Test
    public void required_validate_boundedDepthOfOnNextAndRequestRecursion() throws Exception {
        super.required_validate_boundedDepthOfOnNextAndRequestRecursion();
    }

    @Override
    @Test
    public void required_createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable {
        super.required_createPublisher1MustProduceAStreamOfExactly1Element();
    }

    @Override
    @Test
    public void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
        super.required_createPublisher3MustProduceAStreamOfExactly3Elements();
    }

    @Override
    @Test
    public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements()
            throws Throwable {
        super.required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements();
    }

    @Override
    @Test
    public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
        super.required_spec102_maySignalLessThanRequestedAndTerminateSubscription();
    }

    @Override
    @Test
    public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
        super.stochastic_spec103_mustSignalOnMethodsSequentially();
    }

    @Override
    @Test
    public void optional_spec104_mustSignalOnErrorWhenFails() throws Throwable {
        super.optional_spec104_mustSignalOnErrorWhenFails();
    }

    @Override
    @Test
    public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
        super.required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates();
    }

    @Override
    @Test
    public void optional_spec105_emptyStreamMustTerminateBySignallingOnComplete() throws Throwable {
        super.optional_spec105_emptyStreamMustTerminateBySignallingOnComplete();
    }

    @Override
    @Test
    public void untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled()
            throws Throwable {
        super.required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled();
    }

    @Override
    @Test
    public void untested_spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec109_subscribeShouldNotThrowNonFatalThrowable() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec109_subscribeThrowNPEOnNullSubscriber() throws Throwable {
        super.required_spec109_subscribeThrowNPEOnNullSubscriber();
    }

    @Override
    public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe()
            throws Throwable {
        super.required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe();
    }

    @Override
    @Test
    public void required_spec109_mustIssueOnSubscribeForNonNullSubscriber() throws Throwable {
        super.required_spec109_mustIssueOnSubscribeForNonNullSubscriber();
    }

    @Override
    @Test
    public void untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice()
            throws Throwable {
        super.untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice();
    }

    @Override
    @Test
    public void optional_spec111_maySupportMultiSubscribe() throws Throwable {
        super.optional_spec111_maySupportMultiSubscribe();
    }

    @Override
    @Test
    public void optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals()
            throws Throwable {
        super.optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals();
    }

    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne()
            throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne();
    }

    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront()
            throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront();
    }

    @Override
    public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected()
            throws Throwable {
        super.optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected();
    }

    @Override
    @Test
    public void required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe()
            throws Throwable {
        super.required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe();
    }

    @Override
    @Test
    public void required_spec303_mustNotAllowUnboundedRecursion() throws Throwable {
        super.required_spec303_mustNotAllowUnboundedRecursion();
    }

    @Override
    @Test
    public void untested_spec304_requestShouldNotPerformHeavyComputations() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec305_cancelMustNotSynchronouslyPerformHeavyComputation() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable {
        super.required_spec306_afterSubscriptionIsCancelledRequestMustBeNops();
    }

    @Override
    @Test
    public void required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops()
            throws Throwable {
        super.required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops();
    }

    @Override
    @Test
    public void required_spec309_requestZeroMustSignalIllegalArgumentException() throws Throwable {
        super.required_spec309_requestZeroMustSignalIllegalArgumentException();
    }

    @Override
    @Test
    public void required_spec309_requestNegativeNumberMustSignalIllegalArgumentException()
            throws Throwable {
        super.required_spec309_requestNegativeNumberMustSignalIllegalArgumentException();
    }

    @Override
    @Test
    public void optional_spec309_requestNegativeNumberMaySignalIllegalArgumentExceptionWithSpecificMessage()
            throws Throwable {
        super.optional_spec309_requestNegativeNumberMaySignalIllegalArgumentExceptionWithSpecificMessage();
    }

    @Override
    @Test
    public void required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling()
            throws Throwable {
        super.required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling();
    }

    @Override
    @Test
    public void required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber()
            throws Throwable {
        super.required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber();
    }

    @Override
    @Test
    public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
        super.required_spec317_mustSupportAPendingElementCountUpToLongMaxValue();
    }

    @Override
    @Test
    public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue()
            throws Throwable {
        super.required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue();
    }

    @Override
    @Test
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
        super.required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue();
    }

    @Override
    @Test
    public void required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError()
            throws Throwable {
        super.required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError();
    }

    @Override
    @Test
    public void mustImmediatelyPassOnOnErrorEventsReceivedFromItsUpstreamToItsDownstream()
            throws Exception {
        super.mustImmediatelyPassOnOnErrorEventsReceivedFromItsUpstreamToItsDownstream();
    }

    @Override
    @Test
    public void required_exerciseWhiteboxHappyPath() throws Throwable {
        super.required_exerciseWhiteboxHappyPath();
    }

    @Override
    @Test
    public void required_spec201_mustSignalDemandViaSubscriptionRequest() throws Throwable {
        super.required_spec201_mustSignalDemandViaSubscriptionRequest();
    }

    @Override
    @Test
    public void untested_spec202_shouldAsynchronouslyDispatch() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete()
            throws Throwable {
        super.required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
    }

    @Override
    @Test
    public void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError()
            throws Throwable {
        super.required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
    }

    @Override
    @Test
    public void untested_spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() {
        // Do nothing - untested
    }

    @Override
    public void required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal()
            throws Throwable {
        super.required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal();
    }

    @Override
    @Test
    public void untested_spec206_mustCallSubscriptionCancelIfItIsNoLongerValid() {
        // Do nothing - untested
    }

    @Override
    public void untested_spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel()
            throws Throwable {
        super.required_spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel();
    }

    @Override
    @Test
    public void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall()
            throws Throwable {
        super.required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall();
    }

    @Override
    @Test
    public void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall()
            throws Throwable {
        super.required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall();
    }

    @Override
    @Test
    public void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall()
            throws Throwable {
        super.required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall();
    }

    @Override
    @Test
    public void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall()
            throws Throwable {
        super.required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall();
    }

    @Override
    public void untested_spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality_specViolation() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec213_failingOnSignalInvocation() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec213_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull()
            throws Throwable {
        super.required_spec213_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull();
    }

    @Override
    @Test
    public void required_spec213_onNext_mustThrowNullPointerExceptionWhenParametersAreNull()
            throws Throwable {
        super.required_spec213_onNext_mustThrowNullPointerExceptionWhenParametersAreNull();
    }

    @Override
    @Test
    public void required_spec213_onError_mustThrowNullPointerExceptionWhenParametersAreNull()
            throws Throwable {
        super.required_spec213_onError_mustThrowNullPointerExceptionWhenParametersAreNull();
    }

    @Override
    @Test
    public void untested_spec301_mustNotBeCalledOutsideSubscriberContext() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec308_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable {
        super.required_spec308_requestMustRegisterGivenNumberElementsToBeProduced();
    }

    @Override
    @Test
    public void untested_spec310_requestMaySynchronouslyCallOnNextOnSubscriber() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec311_requestMaySynchronouslyCallOnCompleteOrOnError() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec314_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec315_cancelMustNotThrowExceptionAndMustSignalOnError() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec316_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_mustRequestFromUpstreamForElementsThatHaveBeenRequestedLongAgo()
            throws Throwable {
        super.required_mustRequestFromUpstreamForElementsThatHaveBeenRequestedLongAgo();
    }

    @Override
    public void notVerified() {
        System.err.println("Not verified by this TCK");
    }

    @Override
    public void notVerified(String message) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        System.err.println("[Not Verified - " + element.getMethodName() + "] " + message);
    }
}