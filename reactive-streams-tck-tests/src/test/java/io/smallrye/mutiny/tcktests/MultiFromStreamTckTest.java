package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;
import java.util.stream.LongStream;

import io.smallrye.mutiny.Multi;

public class MultiFromStreamTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return Multi.createFrom().items(() -> LongStream.rangeClosed(1, elements).boxed());
    }
}
