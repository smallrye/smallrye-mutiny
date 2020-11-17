package tck;

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiFromGeneratorTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().generator(AtomicLong::new, (counter, emitter) -> {
            emitter.emit(counter.getAndIncrement());
            if (counter.get() == elements) {
                emitter.complete();
            }
            return counter;
        });
    }
}
