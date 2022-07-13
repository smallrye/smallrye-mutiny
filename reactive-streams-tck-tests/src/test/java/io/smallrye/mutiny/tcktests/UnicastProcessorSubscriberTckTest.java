package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

public class UnicastProcessorSubscriberTckTest extends AbstractBlackBoxSubscriberTck {

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber() {
        return UnicastProcessor.create();
    }

}
