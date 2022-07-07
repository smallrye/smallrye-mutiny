package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.TestException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;

public class CountOperatorTckTest extends PublisherVerification<Long> {
    public CountOperatorTckTest() {
        super(new TestEnvironment(100));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        if (elements == 0L) {
            return Multi.createFrom().empty();
        }
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        return multi.plug(Math.count());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().failure(new TestException())
                .plug(Math.count());
    }
}
