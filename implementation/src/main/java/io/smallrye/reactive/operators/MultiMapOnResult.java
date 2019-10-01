package io.smallrye.reactive.operators;

import java.util.function.Function;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

public class MultiMapOnResult<T, R> extends MultiOperator<T, R> {
    private final Function<? super T, ? extends R> mapper;

    public MultiMapOnResult(Multi<T> upstream, Function<? super T, ? extends R> mapper) {
        super(upstream);
        this.mapper = mapper;
    }

    @Override
    protected Flowable<R> flowable() {
        return upstreamAsFlowable().map(mapper::apply);
    }
}
