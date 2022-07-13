package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import java.util.concurrent.Flow.Publisher;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.reactivestreams.tck.flow.support.TestException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;
import io.smallrye.mutiny.math.Statistic;

public class StatisticsTckTest extends FlowPublisherVerification<Statistic<Long>> {
    public StatisticsTckTest() {
        super(new TestEnvironment(100));
    }

    @Override
    public Publisher<Statistic<Long>> createFlowPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        return multi
                .plug(Math.statistics())
                .skip().where(s -> s.getMin() == null); // Skip the value send on empty streams
    }

    @Override
    public Publisher<Statistic<Long>> createFailedFlowPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.statistics())
                .skip().where(s -> s.getMin() == null);
    }
}
