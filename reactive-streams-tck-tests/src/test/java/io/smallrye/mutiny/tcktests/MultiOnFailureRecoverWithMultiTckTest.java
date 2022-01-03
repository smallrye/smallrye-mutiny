package io.smallrye.mutiny.tcktests;

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiOnFailureRecoverWithMultiTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long l) {
        return failedUpstream()
                .onFailure().recoverWithMulti(t -> Multi.createFrom().items(LongStream.rangeClosed(1, l).boxed()));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return failedUpstream()
                .onFailure().recoverWithMulti(t -> {
                    // Re-throw the exception.
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    // Wrap if required.
                    throw new RuntimeException(t);
                });
    }

}
