package tck;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.reactivestreams.Publisher;

public class MultiRunSubscriptionOnTckTest extends AbstractPublisherTck<Long> {

    private ExecutorService executor;

    @BeforeEach
    public void init() {
        executor = Executors.newFixedThreadPool(3);
    }

    @AfterEach
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
