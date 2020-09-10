package tck;

import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiFromCollectionTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
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
