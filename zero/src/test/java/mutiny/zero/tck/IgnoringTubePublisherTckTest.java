package mutiny.zero.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import mutiny.zero.BackpressureStrategy;
import mutiny.zero.ZeroPublisher;

public class IgnoringTubePublisherTckTest extends PublisherVerification<Long> {

    public IgnoringTubePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return ZeroPublisher.create(BackpressureStrategy.IGNORE, -1, tube -> {
            new Thread(() -> {
                long n = 0L;
                while (n < elements && !tube.cancelled()) {
                    if (tube.outstandingRequests() > 0L) {
                        tube.send(n++);
                    }
                }
                if (!tube.cancelled()) {
                    tube.complete();
                }
            }).start();
        });
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }
}
