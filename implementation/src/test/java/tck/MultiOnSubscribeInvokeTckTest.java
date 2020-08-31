package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiOnSubscribeInvokeTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().iterable(iterate(elements))
                .onSubscribe().invoke(x -> {
                    // noop
                });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onSubscribe().invoke(x -> {
                    // noop
                });
    }
}
