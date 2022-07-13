package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;
import java.util.stream.LongStream;

import io.smallrye.mutiny.Multi;

public class MultiOnFailureRecoverWithItemTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long l) {
        return failedUpstream()
                .onFailure().recoverWithMulti(t -> Multi.createFrom().items(LongStream.rangeClosed(1, l).boxed()));
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return failedUpstream()
                .onFailure().recoverWithItem(t -> {
                    // Re-throw the exception.
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    // Wrap if required.
                    throw new RuntimeException(t);
                });
    }

}
