package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import org.testng.annotations.Ignore;

import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

public class BroadcastSerializedProcessorSubscriberTckTest extends AbstractBlackBoxSubscriberTck {

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber() {
        return BroadcastProcessor.<Integer> create().serialized();
    }

    @Override
    @Ignore
    public void required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() {
        // Ignoring test
        // The broadcast processor is able to handle multiple subscription.
    }
}
