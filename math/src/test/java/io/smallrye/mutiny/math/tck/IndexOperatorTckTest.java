package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.TestException;
import org.reactivestreams.tck.junit5.PublisherVerification;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;
import io.smallrye.mutiny.tuples.Tuple2;

public class IndexOperatorTckTest extends PublisherVerification<Tuple2<Long, Long>> {
    public IndexOperatorTckTest() {
        super(new TestEnvironment(100));
    }

    @Override
    public Publisher<Tuple2<Long, Long>> createPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        return multi.plug(Math.index());
    }

    @Override
    public Publisher<Tuple2<Long, Long>> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.index());
    }
}
