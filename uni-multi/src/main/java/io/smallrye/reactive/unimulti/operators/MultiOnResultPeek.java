package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

public class MultiOnResultPeek<T> extends MultiOperator<T, T> {
    private final Consumer<? super T> callback;

    public MultiOnResultPeek(Multi<T> upstream, Consumer<? super T> callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().doOnNext(callback::accept);
    }
}
