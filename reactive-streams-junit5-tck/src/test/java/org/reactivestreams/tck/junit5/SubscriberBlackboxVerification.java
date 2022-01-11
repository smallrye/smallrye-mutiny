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
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactivestreams.tck.TestEnvironment;

/**
 * Provides tests for verifying {@link org.reactivestreams.Subscriber} and {@link org.reactivestreams.Subscription}
 * specification rules, without any modifications to the tested implementation (also known as "Black Box" testing).
 * <p>
 * This verification is NOT able to check many of the rules of the spec, and if you want more
 * verification of your implementation you'll have to implement {@code org.reactivestreams.tck.SubscriberWhiteboxVerification}
 * instead.
 *
 * @see org.reactivestreams.Subscriber
 * @see org.reactivestreams.Subscription
 */
@ExtendWith(TestngSkippingExtension.class)
public abstract class SubscriberBlackboxVerification<T>
        extends org.reactivestreams.tck.SubscriberBlackboxVerification<T> {

    protected SubscriberBlackboxVerification(TestEnvironment env) {
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
    public void required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest() throws Throwable {
        super.required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest();
    }

    @Override
    @Test
    public void untested_spec202_blackbox_shouldAsynchronouslyDispatch() throws Exception {
        // Untested - do nothing
    }

    @Override
    @Test
    public void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete()
            throws Throwable {
        super.required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete();
    }

    @Override
    @Test
    public void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError()
            throws Throwable {
        super.required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError();
    }

    @Override
    public void untested_spec204_blackbox_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError()
            throws Exception {
        super.untested_spec204_blackbox_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError();
    }

    @Override
    public void required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal()
            throws Exception {
        super.required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal();
    }

    @Override
    @Test
    public void untested_spec206_blackbox_mustCallSubscriptionCancelIfItIsNoLongerValid() {
        // Do nothing - untested
    }

    @Override
    public void untested_spec207_blackbox_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization() {
        // Do nothing - untested
    }

    @Override
    public void untested_spec208_blackbox_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall()
            throws Throwable {
        super.required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall();
    }

    @Override
    public void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall()
            throws Throwable {
        super.required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall();
    }

    @Override
    @Test
    public void required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall()
            throws Throwable {
        super.required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall();
    }

    @Override
    @Test
    public void required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall()
            throws Throwable {
        super.required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall();
    }

    @Override
    public void untested_spec211_blackbox_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec212_blackbox_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec213_blackbox_failingOnSignalInvocation() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void required_spec213_blackbox_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull()
            throws Throwable {
        super.required_spec213_blackbox_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull();
    }

    @Override
    @Test
    public void required_spec213_blackbox_onNext_mustThrowNullPointerExceptionWhenParametersAreNull()
            throws Throwable {
        super.required_spec213_blackbox_onNext_mustThrowNullPointerExceptionWhenParametersAreNull();
    }

    @Override
    @Test
    public void required_spec213_blackbox_onError_mustThrowNullPointerExceptionWhenParametersAreNull()
            throws Throwable {
        super.required_spec213_blackbox_onError_mustThrowNullPointerExceptionWhenParametersAreNull();
    }

    @Override
    @Test
    public void untested_spec301_blackbox_mustNotBeCalledOutsideSubscriberContext() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec308_blackbox_requestMustRegisterGivenNumberElementsToBeProduced() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec310_blackbox_requestMaySynchronouslyCallOnNextOnSubscriber() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec311_blackbox_requestMaySynchronouslyCallOnCompleteOrOnError() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec314_blackbox_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec315_blackbox_cancelMustNotThrowExceptionAndMustSignalOnError() {
        // Do nothing - untested
    }

    @Override
    @Test
    public void untested_spec316_blackbox_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() {
        // Do nothing - untested
    }

    @Override
    public void notVerified() {
        System.err.println("Not verified by this TCK");
    }
}