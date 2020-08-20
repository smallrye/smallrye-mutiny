package tck;

import org.reactivestreams.Subscriber;
import org.testng.annotations.Ignore;

import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

public class BroadcastProcessorSubscriberTckTest extends AbstractBlackBoxSubscriberTck {

    @Override
    public Subscriber<Integer> createSubscriber() {
        return BroadcastProcessor.create();
    }

    @Override
    @Ignore
    public void required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal()
            throws Exception {
        // Ignoring test
        // The broadcast processor is able to handle multiple subscription.
    }
}
