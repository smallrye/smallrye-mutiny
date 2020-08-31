package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiOnCompletionInvokeUniTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().iterable(iterate(elements))
                .onCompletion().invokeUni(() -> Uni.createFrom().nullItem());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onCompletion().invokeUni(() -> Uni.createFrom().nullItem());
    }
}
