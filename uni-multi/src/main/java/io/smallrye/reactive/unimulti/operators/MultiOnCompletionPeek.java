package io.smallrye.reactive.unimulti.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

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
