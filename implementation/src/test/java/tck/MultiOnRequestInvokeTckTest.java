package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiOnRequestInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().iterable(iterate(elements))
                .onRequest().invoke((count) -> {
                    // Do nothing
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onRequest().invoke((count) -> {
                    // noop
                });
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

}
