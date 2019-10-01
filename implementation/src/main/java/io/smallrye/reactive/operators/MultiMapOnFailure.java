package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

import java.util.function.Function;
import java.util.function.Predicate;

import static io.smallrye.reactive.helpers.ParameterValidation.MAPPER_RETURNED_NULL;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiMapOnFailure<T> extends MultiOperator<T, T> {
    private final Predicate<? super Throwable> predicate;
    private final Function<? super Throwable, ? extends Throwable> mapper;

    public MultiMapOnFailure(Multi<T> upstream, Predicate<? super Throwable> predicate,
            Function<? super Throwable, ? extends Throwable> mapper) {
        super(nonNull(upstream, "upstream"));
        this.predicate = predicate == null ? x -> true : predicate;
        this.mapper = nonNull(mapper, "mapper");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().onErrorResumeNext(failure -> {
            if (predicate.test(failure)) {
                Throwable throwable = mapper.apply(failure);
                if (throwable == null) {
                    return Flowable.error(new NullPointerException(MAPPER_RETURNED_NULL));
                } else {
                    return Flowable.error(throwable);
                }
            }
            return Flowable.error(failure);
        });
    }
}
