package io.smallrye.mutiny.tcktests;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

public class MultiRunSubscriptionOnTckTest extends AbstractPublisherTck<Long> {

    private ExecutorService executor;

    @BeforeTest
    public void init() {
        executor = Executors.newFixedThreadPool(3);
    }

    @AfterTest
    public void cleanup() {
        executor.shutdown();
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return upstream(elements)
                .runSubscriptionOn(executor);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .runSubscriptionOn(executor);
    }
}
