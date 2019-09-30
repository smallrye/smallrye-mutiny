package io.smallrye.reactive.unimulti.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

public class MultiIgnore<T> extends MultiOperator<T, Void> {

    public MultiIgnore(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    protected Flowable<Void> flowable() {
        return upstreamAsFlowable().ignoreElements().toFlowable();
    }
}
