package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiOnCompletionInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().iterable(iterate(elements))
                .onCompletion().invoke(() -> {
                    // Do nothing
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onCompletion().invoke(() -> {
                    // noop
                });
    }
}
