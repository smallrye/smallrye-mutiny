package io.smallrye.mutiny.tcktests;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Multi;

public class MultiFromGeneratorTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return Multi.createFrom().generator(AtomicLong::new, (counter, emitter) -> {
            emitter.emit(counter.getAndIncrement());
            if (counter.get() == elements) {
                emitter.complete();
            }
            return counter;
        });
    }
}
