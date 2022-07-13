package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberWhiteboxVerification;

import io.smallrye.mutiny.subscription.MultiSubscriber;

public abstract class AbstractWhiteBoxSubscriberTck extends FlowSubscriberWhiteboxVerification<Integer> {

    public AbstractWhiteBoxSubscriberTck() {
        this(100);
    }

    public AbstractWhiteBoxSubscriberTck(long timeout) {
        super(new TestEnvironment(timeout));
    }

    public MultiSubscriber<Integer> createReportingDownstreamSubscriber(
            SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<Integer> probe) {
        return new MultiSubscriber<Integer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                probe.registerOnSubscribe(new SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long elements) {
                        subscription.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        subscription.cancel();
                    }
                });
            }

            @Override
            public void onItem(Integer item) {
                probe.registerOnNext(item);
            }

            @Override
            public void onFailure(Throwable failure) {
                probe.registerOnError(failure);

            }

            @Override
            public void onCompletion() {
                probe.registerOnComplete();
            }

        };
    }

    @Override
    public Integer createElement(int i) {
        return i;
    }

}
