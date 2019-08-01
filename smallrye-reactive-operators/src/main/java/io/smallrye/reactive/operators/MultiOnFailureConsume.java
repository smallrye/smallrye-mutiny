package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiOnFailureConsume<T> extends MultiOperator<T, T> {
    private final Consumer<Throwable> callback;
    private final Predicate<? super Throwable> predicate;

    public MultiOnFailureConsume(Multi<T> upstream, Consumer<Throwable> callback, Predicate<? super Throwable> predicate) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
        this.predicate = predicate == null ? x -> true : predicate;
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().doOnError(failure -> {
            if (predicate.test(failure)) {
                callback.accept(failure);
            }
        });
    }
}
