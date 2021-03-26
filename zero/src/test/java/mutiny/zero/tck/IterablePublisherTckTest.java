package mutiny.zero.tck;

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import mutiny.zero.ZeroPublisher;

public class IterablePublisherTckTest extends PublisherVerification<Long> {

    public IterablePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return ZeroPublisher.fromItems(list);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024L;
    }
}
