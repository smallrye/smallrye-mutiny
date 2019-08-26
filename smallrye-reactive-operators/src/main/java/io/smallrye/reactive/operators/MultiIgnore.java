package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

public class MultiIgnore<T> extends MultiOperator<T, Void> {

    public MultiIgnore(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    protected Flowable<Void> flowable() {
        return upstreamAsFlowable().ignoreElements().toFlowable();
    }
}
