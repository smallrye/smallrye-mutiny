package io.smallrye.mutiny.tcktests;

import org.reactivestreams.Publisher;
import org.testng.annotations.Ignore;

import io.smallrye.mutiny.Multi;

public class MultiReplayTckTest extends AbstractPublisherTck<Long> {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Multi<Long> upstream = upstream(elements);
        return Multi.createBy().replaying().ofMulti(upstream);
    }

    @Override
    @Ignore
    public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() {
        // The broadcast is capping at Long.MAX.
    }
}
