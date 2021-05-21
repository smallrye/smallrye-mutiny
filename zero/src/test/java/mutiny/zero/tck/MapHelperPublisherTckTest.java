package mutiny.zero.tck;

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import mutiny.zero.PublisherHelpers;
import mutiny.zero.ZeroPublisher;

public class MapHelperPublisherTckTest extends PublisherVerification<Long> {

    public MapHelperPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return PublisherHelpers.map(ZeroPublisher.fromItems(list), n -> n / 2L);
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
