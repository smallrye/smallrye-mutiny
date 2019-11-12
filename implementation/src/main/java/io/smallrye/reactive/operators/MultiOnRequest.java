package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.function.LongConsumer;

import io.smallrye.reactive.Multi;
import org.reactivestreams.Publisher;

public class MultiOnRequest<T> extends MultiOperator<T, T> {
    private final LongConsumer consumer;

    public MultiOnRequest(Multi<T> upstream, LongConsumer consumer) {
        super(nonNull(upstream, "upstream"));
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected Publisher<T> publisher() {
        return upstreamAsFlowable().doOnRequest(consumer::accept);
    }
}
