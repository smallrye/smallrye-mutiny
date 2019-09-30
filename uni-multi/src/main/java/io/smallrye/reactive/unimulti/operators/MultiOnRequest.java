package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.function.LongConsumer;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

public class MultiOnRequest<T> extends MultiOperator<T, T> {
    private final LongConsumer consumer;

    public MultiOnRequest(Multi<T> upstream, LongConsumer consumer) {
        super(nonNull(upstream, "upstream"));
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().doOnRequest(consumer::accept);
    }
}
