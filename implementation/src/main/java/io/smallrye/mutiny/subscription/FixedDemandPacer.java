package io.smallrye.mutiny.subscription;

import java.time.Duration;

import io.smallrye.common.annotation.Experimental;

/**
 * A demand pacer with a fixed delay / fixed demand.
 */
@Experimental("Demand pacing is a new experimental API introduced in Mutiny 1.5.0")
public class FixedDemandPacer implements DemandPacer {

    private final Request request;

    public FixedDemandPacer(long demand, Duration delay) {
        request = new Request(demand, delay);
    }

    @Override
    public Request initial() {
        return request;
    }

    @Override
    public Request apply(Request previousRequest, long observedItemsCount) {
        return request;
    }
}
