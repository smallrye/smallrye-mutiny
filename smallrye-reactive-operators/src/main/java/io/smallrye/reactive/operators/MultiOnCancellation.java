package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiOnCancellation<T> extends MultiOperator<T, T> {
    private final Runnable callback;

    public MultiOnCancellation(Multi<T> upstream, Runnable callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().doOnCancel(callback::run);
    }
}
