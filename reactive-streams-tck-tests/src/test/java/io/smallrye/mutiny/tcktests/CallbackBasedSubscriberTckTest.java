package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.subscription.Subscribers;

public class CallbackBasedSubscriberTckTest extends AbstractWhiteBoxSubscriberTck {

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        return new Subscribers.CallbackBasedSubscriber<>(
                Context.empty(),
                probe::registerOnNext,
                probe::registerOnError,
                probe::registerOnComplete,
                subscription -> probe.registerOnSubscribe(new SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long elements) {
                        subscription.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        subscription.cancel();
                    }
                })

        );
    }
}
