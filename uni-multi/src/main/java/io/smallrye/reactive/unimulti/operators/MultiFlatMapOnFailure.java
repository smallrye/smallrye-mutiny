package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.MAPPER_RETURNED_NULL;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

public class MultiFlatMapOnFailure<T> extends MultiOperator<T, T> {
    private final Predicate<? super Throwable> predicate;
    private final Function<? super Throwable, ? extends Multi<? extends T>> mapper;

    public MultiFlatMapOnFailure(Multi<T> upstream, Predicate<? super Throwable> predicate,
            Function<? super Throwable, ? extends Multi<? extends T>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.predicate = predicate == null ? x -> true : predicate;
        this.mapper = nonNull(mapper, "mapper");
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().onErrorResumeNext(failure -> {
            if (predicate.test(failure)) {
                Publisher<? extends T> res = mapper.apply(failure);
                if (res == null) {
                    return Flowable.error(new NullPointerException(MAPPER_RETURNED_NULL));
                } else {
                    return res;
                }
            }
            return Flowable.error(failure);
        });
    }
}
