package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

import java.util.function.Function;

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
