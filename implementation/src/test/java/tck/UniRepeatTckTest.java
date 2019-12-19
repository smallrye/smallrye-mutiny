package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniRepeatTckTest extends AbstractPublisherTck<Integer> {
    @Override
    public Publisher<Integer> createPublisher(long elements) {
        if (elements == 0) {
            return Multi.createFrom().empty();
        }
        return Uni.createFrom().item(1).repeat().atMost(elements);
    }
}
