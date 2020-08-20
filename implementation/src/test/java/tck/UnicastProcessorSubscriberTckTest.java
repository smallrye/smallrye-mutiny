package tck;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

public class UnicastProcessorSubscriberTckTest extends AbstractBlackBoxSubscriberTck {

    @Override
    public Subscriber<Integer> createSubscriber() {
        return UnicastProcessor.create();
    }

}
