package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiFromResourceTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().resource(() -> elements, max -> {
            int bound = max.intValue();
            return upstream(bound);
        }).withFinalizer(x -> {
            return Uni.createFrom().item(() -> null);
        })
                .onItem().transform(i -> i);
    }

}
