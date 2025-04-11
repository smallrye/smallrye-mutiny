package io.smallrye.mutiny.tcktests;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.stream.LongStream;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.Gatherer.Extraction;

public class MultiGatherTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return Multi.createFrom().iterable(() -> LongStream.rangeClosed(1, elements).boxed().iterator())
                .onItem().gather()
                .into(ArrayList<Long>::new)
                .accumulate((list, next) -> {
                    list.add(next);
                    return list;
                })
                .extract((list, completed) -> {
                    // Note: it might not be obvious at first sight, but with an early completion the list might be empty
                    if (list.isEmpty()) {
                        return Optional.empty();
                    } else {
                        return Optional.of(Extraction.of(new ArrayList<>(), list.get(0)));
                    }
                })
                .finalize(list -> Optional.empty());
    }
}
