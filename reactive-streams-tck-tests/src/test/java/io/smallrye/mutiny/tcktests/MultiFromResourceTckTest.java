package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiFromResourceTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return Multi.createFrom().resource(() -> elements, max -> {
            int bound = max.intValue();
            return upstream(bound);
        }).withFinalizer(x -> {
            return Uni.createFrom().item(() -> null);
        })
                .onItem().transform(i -> i);
    }

}
