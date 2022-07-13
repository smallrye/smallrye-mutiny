package io.smallrye.mutiny.tcktests;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;

public abstract class AbstractBlackBoxSubscriberTck extends FlowSubscriberBlackboxVerification<Integer> {

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
