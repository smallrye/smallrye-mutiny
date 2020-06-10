package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniStreamPublisherTckTest extends AbstractPublisherTck<Integer> {

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return Uni.createFrom().item(elements)
                .onItem().applyMulti(max -> Multi.createFrom().range(0, (int) elements));
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return Uni.createFrom().<Integer> failure(new RuntimeException("failed"))
                .onItem().applyMulti(max -> Multi.createFrom().range(0, 100));
    }
}
