package tck;

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiOnFailureRecoverWithItemTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long l) {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onFailure().recoverWithMulti(t -> Multi.createFrom().items(LongStream.rangeClosed(1, l).boxed()));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
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
