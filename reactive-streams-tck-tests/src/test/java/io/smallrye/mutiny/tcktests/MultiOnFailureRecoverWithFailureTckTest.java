package io.smallrye.mutiny.tcktests;

import java.util.function.Function;

import org.reactivestreams.Publisher;

public class MultiOnFailureRecoverWithFailureTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return upstream(elements)
                .onFailure().recoverWithMulti(t -> {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new RuntimeException(t);
                })
                .map(Function.identity());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
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
