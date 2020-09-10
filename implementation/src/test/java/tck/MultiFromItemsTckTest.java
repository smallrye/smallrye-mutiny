package tck;

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiFromItemsTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return Multi.createFrom().deferred(() -> Multi.createFrom().items(list));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
