package io.smallrye.mutiny.tcktests;

import java.util.List;
import java.util.concurrent.Flow.Publisher;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import io.smallrye.mutiny.Multi;

public class MultiFromIterableTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        List<Long> list = LongStream.rangeClosed(1, elements).boxed().collect(Collectors.toList());
        return Multi.createFrom().deferred(() -> Multi.createFrom().iterable(list));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

}
