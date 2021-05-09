package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.TestException;
import org.reactivestreams.tck.junit5.PublisherVerification;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;
import io.smallrye.mutiny.math.Statistic;

public class StatisticsTckTest extends PublisherVerification<Statistic<Long>> {
    public StatisticsTckTest() {
        super(new TestEnvironment(100));
    }

    @Override
    public Publisher<Statistic<Long>> createPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        return multi
                .plug(Math.statistics())
                .skip().where(s -> s.getMin() == null); // Skip the value send on empty streams
    }

    @Override
    public Publisher<Statistic<Long>> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.statistics())
                .skip().where(s -> s.getMin() == null);
    }
}
