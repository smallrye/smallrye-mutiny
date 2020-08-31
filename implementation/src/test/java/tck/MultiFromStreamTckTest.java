package tck;

import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiFromStreamTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        Stream<Long> list = LongStream.rangeClosed(1, elements).boxed();
        return Multi.createFrom().items(list);
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
