package tck;

import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public abstract class AbstractBlackBoxSubscriberTck extends SubscriberBlackboxVerification<Integer> {

    public AbstractBlackBoxSubscriberTck() {
        this(100);
    }

    public AbstractBlackBoxSubscriberTck(long timeout) {
        super(new TestEnvironment(timeout));
    }

    @Override
    public Integer createElement(int i) {
        return i;
    }

}
