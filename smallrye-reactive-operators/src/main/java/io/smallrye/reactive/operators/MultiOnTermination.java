package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.tuples.Functions;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiOnTermination<T> extends MultiOperator<T, T> {
    private final Functions.TriConsumer<T, Throwable, Boolean> consumer;

    public MultiOnTermination(Multi<T> upstream, Functions.TriConsumer<T, Throwable, Boolean> consumer) {
        super(nonNull(upstream, "upstream"));
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable()
                .doOnCancel(() -> consumer.accept(null, null, true))
                .doOnError(fail -> consumer.accept(null, fail, false))
                .doOnNext(result -> consumer.accept(result, null, false));
    }
}
