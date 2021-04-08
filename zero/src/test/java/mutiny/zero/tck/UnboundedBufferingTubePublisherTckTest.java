package mutiny.zero.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import mutiny.zero.BackpressureStrategy;
import mutiny.zero.ZeroPublisher;

public class UnboundedBufferingTubePublisherTckTest extends PublisherVerification<Long> {

    public UnboundedBufferingTubePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return ZeroPublisher.create(BackpressureStrategy.UNBOUNDED_BUFFER, -1, tube -> TubeEmitLoop.loop(tube, elements));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }
}
