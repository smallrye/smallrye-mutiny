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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.tck.TestEnvironment;

/**
 * Provides whitebox style tests for verifying {@link org.reactivestreams.Subscriber}
 * and {@link org.reactivestreams.Subscription} specification rules.
 *
 * @see org.reactivestreams.Subscriber
 * @see org.reactivestreams.Subscription
 */
public abstract class SubscriberWhiteboxVerification<T>
        extends org.reactivestreams.tck.SubscriberWhiteboxVerification<T> {

    protected SubscriberWhiteboxVerification(TestEnvironment env) {
        super(env);
    }

    @BeforeEach
    public void startPublisherExecutorService() {
        super.startPublisherExecutorService();
    }

    @AfterEach
    public void shutdownPublisherExecutorService() {
        super.shutdownPublisherExecutorService();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
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
    public void untested_spec202_shouldAsynchronouslyDispatch() throws Exception {
        // Untested - do nothing
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
    public void untested_spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError()
            throws Exception {
        // Untested - do nothing
    }

    @Override
    public void required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal()
            throws Throwable {
        super.required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal();
    }

    @Override
    @Test
    public void untested_spec206_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception {
        // Untested - do nothing
    }

    @Override
    public void untested_spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization()
            throws Exception {
        // Untested - do nothing
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
    public void untested_spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents()
            throws Exception {
        // Untested - do nothing
    }

    @Override
    @Test
    public void untested_spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality_specViolation()
            throws Throwable {
        // Untested - do nothing
    }

    @Override
    @Test
    public void untested_spec213_failingOnSignalInvocation() throws Exception {
        // Untested - do nothing
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
    public void untested_spec301_mustNotBeCalledOutsideSubscriberContext() throws Exception {
        // Untested - do nothing
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
    public void notVerified() {
        System.err.println("Not verified by this TCK");
    }

    @Override
    public void notVerified(String message) {
        System.err.println("[Not Verified] " + message);
    }
}