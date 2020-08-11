package tck;

import io.smallrye.mutiny.Multi;
import org.reactivestreams.Publisher;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

public class MultiRunSubscriptionOnTckTest extends AbstractPublisherTck<Long> {

    private ExecutorService executor;

    @BeforeMethod
    public void init() {
        executor = Executors.newFixedThreadPool(3);
    }

    @AfterMethod
    public void cleanup() {
        executor.shutdown();
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().items(LongStream.rangeClosed(1, elements).boxed())
                .runSubscriptionOn(executor);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .runSubscriptionOn(executor);
    }
}
