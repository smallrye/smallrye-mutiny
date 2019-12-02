package tck;

import io.smallrye.mutiny.Multi;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class MultiFromItemsTckTest extends AbstractPublisherTck<Long> {
    @Override
    public Publisher<Long> createPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return Multi.createFrom().items(list);
    }
}
