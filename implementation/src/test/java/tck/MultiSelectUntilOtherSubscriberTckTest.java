package tck;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.MultiSelectFirstUntilOtherOp;

public class MultiSelectUntilOtherSubscriberTckTest extends AbstractBlackBoxSubscriberTck {
    @Override
    public Subscriber<Integer> createSubscriber() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1024);
        MultiSelectFirstUntilOtherOp.TakeUntilMainProcessor<Integer> main = new MultiSelectFirstUntilOtherOp.TakeUntilMainProcessor<>(
                subscriber);
        return new MultiSelectFirstUntilOtherOp.TakeUntilOtherSubscriber<>(main);
    }

}
