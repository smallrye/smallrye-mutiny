package mutiny.zero.tck;

import java.io.IOException;
import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import mutiny.zero.ZeroPublisher;

public class FailurePublisherTckTest extends PublisherVerification<Long> {

    public FailurePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        // This is irrelevant so we just pass some unrelated publisher
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return ZeroPublisher.fromItems(list);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return ZeroPublisher.fromFailure(new IOException("boom"));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024L;
    }
}
