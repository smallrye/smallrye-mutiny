package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.subscription.DemandPauser;

public class MultiDemandPausingTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        DemandPauser pauser = new DemandPauser();
        return upstream(elements).pauseDemand().using(pauser)
                .invoke(pauser::pause)
                .invoke(pauser::resume);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        DemandPauser pauser = new DemandPauser();
        return failedUpstream().pauseDemand().using(pauser)
                .invoke(pauser::pause)
                .invoke(pauser::resume);
    }

}
