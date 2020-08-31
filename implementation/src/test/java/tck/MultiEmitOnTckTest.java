package tck;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiEmitOnTckTest extends AbstractPublisherTck<Long> {

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
        return Multi.createFrom().iterable(iterate(elements))
                .emitOn(executor);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new IllegalStateException("failed"))
                .emitOn(executor);
    }
}
