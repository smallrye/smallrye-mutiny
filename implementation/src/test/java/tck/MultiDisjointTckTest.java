package tck;

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiDisjointTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return Multi.createFrom().item(list)
                .onItem().disjoint();
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
