package tck;

import java.util.concurrent.*;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniFlatMapPublisherTckTest extends AbstractPublisherTck<Integer> {

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return Uni.createFrom().item(elements)
                .onItem().produceMulti(max -> Multi.createFrom().range(0, (int) elements));
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return Uni.createFrom().<Integer> failure(new RuntimeException("failed"))
                .onItem().produceMulti(max -> Multi.createFrom().range(0, 100));
    }
}
