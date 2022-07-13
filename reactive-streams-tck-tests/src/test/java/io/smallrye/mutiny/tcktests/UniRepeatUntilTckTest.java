package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniRepeatUntilTckTest extends AbstractPublisherTck<Integer> {
    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        if (elements == 0) {
            return Multi.createFrom().empty();
        }
        AtomicInteger count = new AtomicInteger();
        return Uni.createFrom().item(1).repeat().until(x -> count.getAndIncrement() >= elements);
    }
}
