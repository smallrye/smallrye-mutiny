package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

public class MultiOnCompletionPeek<T> extends MultiOperator<T, T> {
    private final Runnable callback;

    public MultiOnCompletionPeek(Multi<T> upstream, Runnable callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().doOnComplete(callback::run);
    }
}
