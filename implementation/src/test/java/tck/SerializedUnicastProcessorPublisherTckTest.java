package tck;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.SerializedProcessor;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

public class SerializedUnicastProcessorPublisherTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        SerializedProcessor<Long, Long> processor = UnicastProcessor.<Long> create().serialized();

        multi.subscribe(processor);

        return processor;
    }

    @Override
    public long maxElementsFromPublisher() {
        // Because we store the elements for the other subscribers.
        return 1024;
    }
}
