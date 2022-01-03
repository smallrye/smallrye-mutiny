package io.smallrye.mutiny.tcktests;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.reactivestreams.Publisher;
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
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .runSubscriptionOn(executor);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .runSubscriptionOn(executor);
    }
}
