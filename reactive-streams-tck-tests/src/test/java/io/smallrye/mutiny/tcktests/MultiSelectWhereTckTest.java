package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

public class MultiSelectWhereTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .select().where(l -> true);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .select().where(l -> true);
    }

}
