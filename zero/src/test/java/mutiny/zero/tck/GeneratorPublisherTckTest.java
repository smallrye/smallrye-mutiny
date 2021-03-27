package mutiny.zero.tck;

import java.util.Iterator;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import mutiny.zero.ZeroPublisher;

public class GeneratorPublisherTckTest extends PublisherVerification<Long> {

    public GeneratorPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return ZeroPublisher.fromGenerator(() -> 10L, init -> new Iterator<Long>() {

            long current = init;
            long emitted = 0L;

            @Override
            public boolean hasNext() {
                return emitted < elements;
            }

            @Override
            public Long next() {
                emitted++;
                return current++;
            }
        });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }
}
