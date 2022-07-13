package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import java.util.concurrent.Flow;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.reactivestreams.tck.flow.support.TestException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;
import io.smallrye.mutiny.tuples.Tuple2;

public class IndexOperatorTckTest extends FlowPublisherVerification<Tuple2<Long, Long>> {
    public IndexOperatorTckTest() {
        super(new TestEnvironment(100));
    }

    @Override
    public Flow.Publisher<Tuple2<Long, Long>> createFlowPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        return multi.plug(Math.index());
    }

    @Override
    public Flow.Publisher<Tuple2<Long, Long>> createFailedFlowPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.index());
    }
}
