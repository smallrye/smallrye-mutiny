package tck;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniRepeatUntilTckTest extends AbstractPublisherTck<Integer> {
    @Override
    public Publisher<Integer> createPublisher(long elements) {
        if (elements == 0) {
            return Multi.createFrom().empty();
        }
        AtomicInteger count = new AtomicInteger();
        return Uni.createFrom().item(1).repeat().until(x -> count.getAndIncrement() >= elements);
    }
}
