package io.smallrye.mutiny.zero.tck;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import io.smallrye.mutiny.zero.ZeroPublisher;

public class CompletionStageTckPublisherTest extends PublisherVerification<Long> {

    public CompletionStageTckPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> 69L);
        return ZeroPublisher.fromCompletionStage(future);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException("boom"));
        return ZeroPublisher.fromCompletionStage(future);
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1L;
    }
}
