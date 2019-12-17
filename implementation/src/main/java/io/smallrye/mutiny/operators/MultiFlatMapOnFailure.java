package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnFailureResumeOp;

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
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        Function<? super Throwable, ? extends Publisher<? extends T>> next = failure -> {
            if (predicate.test(failure)) {
                Publisher<? extends T> res = mapper.apply(failure);
                if (res == null) {
                    return Multi.createFrom().failure(new NullPointerException(MAPPER_RETURNED_NULL));
                } else {
                    return res;
                }
            }
            return Multi.createFrom().failure(failure);
        };
        Multi<T> op = Infrastructure.onMultiCreation(new MultiOnFailureResumeOp<>(upstream(), next));
        op.subscribe(subscriber);
    }
}
