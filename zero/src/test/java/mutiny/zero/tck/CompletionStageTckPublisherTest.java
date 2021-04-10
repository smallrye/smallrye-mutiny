package mutiny.zero.tck;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import mutiny.zero.ZeroPublisher;

public class CompletionStageTckPublisherTest extends PublisherVerification<Long> {

    public CompletionStageTckPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return ZeroPublisher.fromCompletionStage(() -> CompletableFuture.supplyAsync(() -> 69L));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return ZeroPublisher.fromCompletionStage(() -> {
            CompletableFuture<Long> future = new CompletableFuture<>();
            future.completeExceptionally(new IOException("boom"));
            return future;
        });
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1L;
    }
}
