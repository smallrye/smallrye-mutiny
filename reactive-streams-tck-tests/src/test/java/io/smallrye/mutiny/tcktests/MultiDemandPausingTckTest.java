package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.subscription.DemandPauser;

public class MultiDemandPausingTckTest extends AbstractPublisherTck<Long> {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        DemandPauser pauser = new DemandPauser();
        AtomicLong count = new AtomicLong();
        return upstream(elements)
                .emitOn(executor)
                .pauseDemand()
                .bufferUnconditionally()
                .using(pauser)
                .invoke(() -> {
                    if (count.incrementAndGet() % 3L == 0) {
                        pauser.pause();
                        executor.schedule(pauser::resume, 50, TimeUnit.MILLISECONDS);
                    }
                })
                .emitOn(executor);

    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        DemandPauser pauser = new DemandPauser();
        AtomicLong count = new AtomicLong();
        return failedUpstream().pauseDemand()
                .bufferUnconditionally()
                .using(pauser)
                .invoke(() -> {
                    if (count.incrementAndGet() % 3L == 0) {
                        pauser.pause();
                        executor.schedule(pauser::resume, 50, TimeUnit.MILLISECONDS);
                    }
                });
    }

}
