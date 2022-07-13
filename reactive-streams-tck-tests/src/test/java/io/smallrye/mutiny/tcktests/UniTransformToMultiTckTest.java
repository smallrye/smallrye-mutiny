package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniTransformToMultiTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return Uni.createFrom().item(elements)
                .onItem().transformToMulti(max -> upstream(elements));
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return Uni.createFrom().<Integer> failure(new RuntimeException("failed"))
                .onItem().transformToMulti(max -> Multi.createFrom().iterable(iterate(100)));
    }
}
