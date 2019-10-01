package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.helpers.ParameterValidation.positive;

import java.util.function.Predicate;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

public class MultiRetryAtMost<T> extends MultiOperator<T, T> {
    private final Predicate<? super Throwable> predicate;
    private final long attempts;

    public MultiRetryAtMost(Multi<T> upstream, Predicate<? super Throwable> predicate, long numberOfAttempts) {
        super(nonNull(upstream, "upstream"));
        this.predicate = predicate;
        this.attempts = positive(numberOfAttempts, "numberOfAttempts");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().retry((count, failure) -> predicate.test(failure) && count <= attempts);
    }
}
