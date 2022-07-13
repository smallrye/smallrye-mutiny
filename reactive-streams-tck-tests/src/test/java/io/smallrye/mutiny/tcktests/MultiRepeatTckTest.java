package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiRepeatTckTest extends AbstractPublisherTck<Integer> {
    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        if (elements == 0) {
            return Multi.createFrom().empty();
        }
        return Multi.createBy().repeating().uni(() -> Uni.createFrom().item(1))
                .atMost(elements);
    }
}
