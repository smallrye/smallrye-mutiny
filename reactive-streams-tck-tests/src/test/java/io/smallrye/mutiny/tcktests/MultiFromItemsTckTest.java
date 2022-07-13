package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;
import java.util.stream.LongStream;

import io.smallrye.mutiny.Multi;

public class MultiFromItemsTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return Multi.createFrom().deferred(() -> Multi.createFrom().items(list));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
