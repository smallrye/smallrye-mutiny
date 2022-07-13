package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import java.util.concurrent.Flow.Publisher;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.reactivestreams.tck.flow.support.TestException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;

public class MinOperatorTckTest extends FlowPublisherVerification<Long> {
    public MinOperatorTckTest() {
        super(new TestEnvironment(100));
    }

    // NOTE: It works as the upstream generates always increasing numbers.

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements))
                .map(l -> l * -1);
        return multi.plug(Math.min());
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.min());
    }
}
