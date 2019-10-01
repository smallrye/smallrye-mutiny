package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

import java.util.function.LongConsumer;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

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
