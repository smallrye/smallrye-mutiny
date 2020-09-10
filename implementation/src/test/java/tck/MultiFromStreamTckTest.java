package tck;

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiFromStreamTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().items(() -> LongStream.rangeClosed(1, elements).boxed());
    }
}
