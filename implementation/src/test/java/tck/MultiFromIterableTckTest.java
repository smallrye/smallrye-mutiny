package tck;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;

public class MultiFromIterableTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        List<Long> list = LongStream.rangeClosed(1, elements).boxed().collect(Collectors.toList());
        return Multi.createFrom().deferred(() -> Multi.createFrom().iterable(list));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

}
