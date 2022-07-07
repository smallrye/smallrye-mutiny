package io.smallrye.mutiny.math.tck;

import static io.smallrye.mutiny.math.tck.TckHelper.iterate;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.TestException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.math.Math;

public class MinOperatorTckTest extends PublisherVerification<Long> {
    public MinOperatorTckTest() {
        super(new TestEnvironment(100));
    }

    // NOTE: It works as the upstream generates always increasing numbers.

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Multi<Long> multi = Multi.createFrom().iterable(iterate(elements))
                .map(l -> l * -1);
        return multi.plug(Math.min());
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new TestException())
                .plug(Math.min());
    }
}
