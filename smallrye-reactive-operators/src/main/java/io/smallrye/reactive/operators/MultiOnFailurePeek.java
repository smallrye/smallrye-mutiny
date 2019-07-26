package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

import java.util.function.Consumer;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiOnFailurePeek<T> extends MultiOperator<T, T> {
    private final Consumer<Throwable> callback;

    public MultiOnFailurePeek(Multi<T> upstream, Consumer<Throwable> callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().doOnError(callback::accept);
    }
}
