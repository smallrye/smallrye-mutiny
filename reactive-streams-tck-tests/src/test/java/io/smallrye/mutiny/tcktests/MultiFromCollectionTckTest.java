package io.smallrye.mutiny.tcktests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiFromCollectionTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < elements; i++) {
            list.add((long) i);
        }
        return Multi.createFrom().deferred(() -> Multi.createFrom().items(list.toArray(new Long[0])));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
