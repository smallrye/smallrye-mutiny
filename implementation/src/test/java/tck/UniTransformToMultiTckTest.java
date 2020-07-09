package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniTransformToMultiTckTest extends AbstractPublisherTck<Integer> {

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return Uni.createFrom().item(elements)
                .onItem().transformToMulti(max -> Multi.createFrom().range(0, (int) elements));
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return Uni.createFrom().<Integer> failure(new RuntimeException("failed"))
                .onItem().transformToMulti(max -> Multi.createFrom().range(0, 100));
    }
}
