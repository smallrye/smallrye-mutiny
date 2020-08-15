package tck;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.subscription.Subscribers;

public class CallbackBasedSubscriberTckTest extends AbstractWhiteBoxSubscriberTck {
    @Override
    public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        return new Subscribers.CallbackBasedSubscriber<>(
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
