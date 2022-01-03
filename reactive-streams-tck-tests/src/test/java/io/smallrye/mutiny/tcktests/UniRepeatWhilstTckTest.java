package io.smallrye.mutiny.tcktests;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniRepeatWhilstTckTest extends AbstractPublisherTck<Integer> {
    @Override
    public Publisher<Integer> createPublisher(long elements) {
        if (elements == 0) {
            return Multi.createFrom().empty();
        }
        AtomicInteger count = new AtomicInteger();
        return Uni.createFrom().item(1).repeat().whilst(x -> count.getAndIncrement() < elements - 1);
    }
}
