package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.TestException;
import org.reactivestreams.tck.junit5.PublisherVerification;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;

public class MedianTckTest extends PublisherVerification<Double> {
    public MedianTckTest() {
        super(new TestEnvironment(100));
    }

    @Override
    public Publisher<Double> createPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements));
        return multi
                .plug(Math.median());
    }

    @Override
    public Publisher<Double> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.median());
    }
}
