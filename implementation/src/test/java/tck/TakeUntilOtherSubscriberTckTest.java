package tck;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.operators.multi.MultiTakeUntilOtherOp;
import io.smallrye.mutiny.test.AssertSubscriber;

public class TakeUntilOtherSubscriberTckTest extends AbstractBlackBoxSubscriberTck {
    @Override
    public Subscriber<Integer> createSubscriber() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1024);
        MultiTakeUntilOtherOp.TakeUntilMainProcessor<Integer> main = new MultiTakeUntilOtherOp.TakeUntilMainProcessor<>(
                subscriber);
        return new MultiTakeUntilOtherOp.TakeUntilOtherSubscriber<>(main);
    }

}
