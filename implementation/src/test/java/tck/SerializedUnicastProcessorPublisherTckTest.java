package tck;

import java.util.stream.IntStream;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.SerializedProcessor;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

public class SerializedUnicastProcessorPublisherTckTest extends AbstractPublisherTck<Integer> {

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        Multi<Integer> multi = Multi.createFrom().items(IntStream.rangeClosed(1, (int) elements).boxed());
        SerializedProcessor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();

        multi.subscribe(processor);

        return processor;

    }
}
