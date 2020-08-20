package tck;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

public class UnicastSerializedProcessorSubscriberTckTest extends AbstractBlackBoxSubscriberTck {

    @Override
    public Subscriber<Integer> createSubscriber() {
        return UnicastProcessor.<Integer> create().serialized();
    }

}
