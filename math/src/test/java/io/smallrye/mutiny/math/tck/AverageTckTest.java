package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import java.util.concurrent.Flow.Publisher;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.reactivestreams.tck.flow.support.TestException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;

public class AverageTckTest extends FlowPublisherVerification<Double> {
    public AverageTckTest() {
        super(new TestEnvironment(100));
    }

    @Override
    public Publisher<Double> createFlowPublisher(long elements) {
        if (elements == 0L) {
            return Multi.createFrom().empty();
        }
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        return multi
                .plug(Math.average());
    }

    @Override
    public Publisher<Double> createFailedFlowPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.average());
    }
}
