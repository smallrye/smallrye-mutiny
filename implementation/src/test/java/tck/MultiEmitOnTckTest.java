package tck;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import io.smallrye.mutiny.Multi;

public class MultiEmitOnTckTest extends AbstractPublisherTck<Long> {

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
        return Multi
                .createFrom().items(LongStream.rangeClosed(1, (int) elements).boxed())
                .emitOn(executor);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new IllegalStateException("failed"))
                .emitOn(executor);
    }
}
