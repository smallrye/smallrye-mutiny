package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import java.util.concurrent.Flow;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.reactivestreams.tck.flow.support.TestException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;

public class MaxOperatorTckTest extends FlowPublisherVerification<Long> {
    public MaxOperatorTckTest() {
        super(new TestEnvironment(100));
    }

    // NOTE: It works as the upstream generates always increasing numbers.

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        return multi.plug(Math.max());
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.max());
    }
}
