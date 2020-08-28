package tck;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.SubscriberBlackboxVerification;

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
