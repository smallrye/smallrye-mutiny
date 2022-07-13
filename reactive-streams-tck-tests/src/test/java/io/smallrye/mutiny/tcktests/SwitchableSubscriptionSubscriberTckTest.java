package io.smallrye.mutiny.tcktests;

import java.util.Objects;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.reactivestreams.tck.SubscriberWhiteboxVerification;

import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

public class SwitchableSubscriptionSubscriberTckTest extends AbstractWhiteBoxSubscriberTck {

    @Override
    public Subscriber<Integer> createFlowSubscriber(SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<Integer> probe) {
        MultiSubscriber<? super Integer> downstream = createReportingDownstreamSubscriber(probe);
        return new SwitchableSubscriptionSubscriber<Integer>(downstream) {

            @Override
            public void onSubscribe(Subscription subscription) {
                // To pass the TCK we need to disable the switch and cancel the second subscription
                if (super.currentUpstream.get() != null) {
                    subscription.cancel();
                }

                probe.registerOnSubscribe(new SubscriberWhiteboxVerification.SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long elements) {
                        subscription.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        subscription.cancel();
                    }
                });
                super.setOrSwitchUpstream(subscription);
            }

            @Override
            public void onItem(Integer item) {
                Objects.requireNonNull(item); // Just here to pass the TCK.
                probe.registerOnNext(item);
            }

        };
    }

}
